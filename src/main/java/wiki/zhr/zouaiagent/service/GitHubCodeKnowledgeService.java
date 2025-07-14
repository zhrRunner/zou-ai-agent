package wiki.zhr.zouaiagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import wiki.zhr.zouaiagent.rag.MyKeywordEnricher;
import wiki.zhr.zouaiagent.rag.MyTokenTextSplitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * GitHub代码知识库服务
 * 负责下载GitHub仓库代码，转换为文档并存储到pgVector知识库
 */
@Service
@Slf4j
public class GitHubCodeKnowledgeService {
    
    @Value("${github.personal_access_token}")
    private String personalAccessToken;
    
    @Resource
    private DynamicPgVectorStoreService dynamicPgVectorStoreService;
    
    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;
    
    @Resource
    private MyKeywordEnricher myKeywordEnricher;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 支持的代码文件扩展名
    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".ts", ".cpp", ".c", ".h", ".cs", ".go", 
            ".php", ".rb", ".swift", ".kt", ".scala", ".rs", ".vue", ".jsx", 
            ".tsx", ".html", ".css", ".scss", ".less", ".sql", ".xml", ".yaml", 
            ".yml", ".json", ".md", ".txt", ".properties", ".sh", ".bat"
    );
    
    // DashScope embedding 批处理限制
    private static final int EMBEDDING_BATCH_SIZE = 20; // 保守一点，设为20而不是25
    
    /**
     * 构建指定GitHub仓库的代码知识库
     * @param owner 仓库所有者
     * @param repository 仓库名称
     * @param branch 分支名称
     * @param enableSplitting 是否启用文档分片（需要消耗token）
     * @param enableEnrichment 是否启用关键词丰富（需要消耗token）
     */
    public void buildCodeKnowledge(String owner, String repository, String branch, 
                                  boolean enableSplitting, boolean enableEnrichment) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("🚀 开始构建代码知识库: {}/{} (branch: {}, 分片: {}, 丰富: {})", 
                    owner, repository, branch, enableSplitting, enableEnrichment);
            
            // 1. 清理现有数据
            String repositoryName = owner + "_" + repository;
            log.info("🧹 清理现有知识库数据: {}", repositoryName);
            dynamicPgVectorStoreService.clearRepository(repositoryName);
            
            // 2. 创建本地存储目录
            Path localCodePath = createLocalDirectory(repositoryName);
            log.info("📁 创建本地存储目录: {}", localCodePath.toAbsolutePath());
            
            // 3. 检查本地缓存
            boolean hasLocalCache = checkLocalCache(localCodePath);
            if (hasLocalCache) {
                log.info("📦 发现本地缓存，将使用已下载的文件");
            }
            
            // 4. 递归获取仓库所有文件
            log.info("🔍 开始扫描仓库文件结构...");
            List<GitHubFile> allFiles = getAllRepositoryFiles(owner, repository, branch, "");
            
            // 5. 过滤代码文件
            List<GitHubFile> codeFiles = allFiles.stream()
                    .filter(file -> isCodeFile(file.getName()) && file.getType().equals("file"))
                    .toList();
            log.info("📋 发现 {} 个代码文件，总文件数: {}", codeFiles.size(), allFiles.size());
            
            // 6. 下载代码文件
            List<Document> documents = downloadCodeFiles(owner, repository, branch, codeFiles, localCodePath, hasLocalCache);
            
            if (documents.isEmpty()) {
                log.warn("⚠️ 没有成功下载任何代码文件");
                return;
            }
            
            // 7. 文档处理（根据参数决定是否进行分割和关键词丰富）
            log.info("⚙️ 开始处理文档...");
            List<Document> processedDocuments = processDocuments(documents, enableSplitting, enableEnrichment);
            
            // 8. 分批存储到向量数据库（重要：解决DashScope批处理限制）
            log.info("💾 开始分批存储到向量数据库...");
            storeDocumentsInBatches(repositoryName, processedDocuments);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 代码知识库构建完成: {}/{}, 耗时: {}ms, 共处理 {} 个文档片段", 
                    owner, repository, duration, processedDocuments.size());
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ 构建代码知识库失败: {}/{}, 耗时: {}ms", owner, repository, duration, e);
            throw new RuntimeException("构建代码知识库失败", e);
        }
    }
    
    /**
     * 构建指定GitHub仓库的代码知识库（兼容旧版本，默认开启分片和丰富）
     */
    public void buildCodeKnowledge(String owner, String repository, String branch) {
        buildCodeKnowledge(owner, repository, branch, true, true);
    }
    
    /**
     * 检查本地缓存是否存在
     */
    private boolean checkLocalCache(Path localCodePath) {
        try {
            if (!Files.exists(localCodePath)) {
                return false;
            }
            
            // 检查是否有文件
            long fileCount = Files.walk(localCodePath)
                    .filter(Files::isRegularFile)
                    .count();
            
            log.info("📁 本地缓存目录存在，包含 {} 个文件", fileCount);
            return fileCount > 0;
        } catch (IOException e) {
            log.warn("检查本地缓存失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 下载代码文件
     */
    private List<Document> downloadCodeFiles(String owner, String repository, String branch, 
                                           List<GitHubFile> codeFiles, Path localCodePath, boolean hasLocalCache) {
        List<Document> documents = new ArrayList<>();
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;
        
        log.info("📥 开始下载代码文件，总计: {} 个", codeFiles.size());
        
        for (int i = 0; i < codeFiles.size(); i++) {
            GitHubFile file = codeFiles.get(i);
            try {
                // 检查本地文件是否已存在
                Path localFilePath = localCodePath.resolve(file.getPath());
                if (hasLocalCache && Files.exists(localFilePath)) {
                    // 使用本地缓存
                    Document document = createDocumentFromLocalFile(owner, repository, branch, file, localFilePath);
                    if (document != null) {
                        documents.add(document);
                        skipCount++;
                        if ((i + 1) % 10 == 0 || i == codeFiles.size() - 1) {
                            log.info("📦 使用缓存进度: {}/{} (成功: {}, 跳过: {}, 失败: {})", 
                                    i + 1, codeFiles.size(), successCount, skipCount, failCount);
                        }
                        continue;
                    }
                }
                
                // 从GitHub下载
                Document document = downloadAndCreateDocument(owner, repository, branch, file, localCodePath);
                if (document != null) {
                    documents.add(document);
                    successCount++;
                } else {
                    failCount++;
                }
                
                // 每10个文件或最后一个文件时打印进度
                if ((i + 1) % 10 == 0 || i == codeFiles.size() - 1) {
                    log.info("📥 下载进度: {}/{} (成功: {}, 跳过: {}, 失败: {})", 
                            i + 1, codeFiles.size(), successCount, skipCount, failCount);
                }
                
            } catch (Exception e) {
                failCount++;
                log.warn("❌ 下载文件失败: {}, 错误: {}", file.getPath(), e.getMessage());
            }
        }
        
        log.info("📥 文件下载完成！成功: {}, 跳过缓存: {}, 失败: {}, 总计: {}", 
                successCount, skipCount, failCount, codeFiles.size());
        
        return documents;
    }
    
    /**
     * 从本地缓存创建Document
     */
    private Document createDocumentFromLocalFile(String owner, String repository, String branch, 
                                                GitHubFile file, Path localFilePath) throws IOException {
        if (!Files.exists(localFilePath)) {
            return null;
        }
        
        String content = Files.readString(localFilePath);
        if (content.trim().isEmpty()) {
            return null;
        }
        
        // 创建Document对象
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("repository", owner + "/" + repository);
        metadata.put("branch", branch);
        metadata.put("file_path", file.getPath());
        metadata.put("file_name", file.getName());
        metadata.put("file_type", getFileExtension(file.getName()));
        metadata.put("file_size", content.length());
        metadata.put("download_url", file.getDownloadUrl());
        metadata.put("local_path", localFilePath.toString());
        metadata.put("from_cache", true);
        
        return new Document(content, metadata);
    }
    
    /**
     * 分批存储文档到向量数据库（解决DashScope批处理限制）
     */
    private void storeDocumentsInBatches(String repositoryName, List<Document> processedDocuments) {
        VectorStore vectorStore = dynamicPgVectorStoreService.getVectorStore(repositoryName);
        
        int totalDocuments = processedDocuments.size();
        int batchCount = (totalDocuments + EMBEDDING_BATCH_SIZE - 1) / EMBEDDING_BATCH_SIZE;
        
        log.info("💾 开始分批存储，总文档: {}, 批次大小: {}, 总批次: {}", 
                totalDocuments, EMBEDDING_BATCH_SIZE, batchCount);
        
        for (int i = 0; i < totalDocuments; i += EMBEDDING_BATCH_SIZE) {
            int endIndex = Math.min(i + EMBEDDING_BATCH_SIZE, totalDocuments);
            List<Document> batch = processedDocuments.subList(i, endIndex);
            int currentBatch = (i / EMBEDDING_BATCH_SIZE) + 1;
            
            try {
                log.info("💾 存储批次 {}/{}: {} 个文档 (索引 {}-{})", 
                        currentBatch, batchCount, batch.size(), i, endIndex - 1);
                
                vectorStore.add(batch);
                
                log.info("✅ 批次 {}/{} 存储成功", currentBatch, batchCount);
                
                // 批次之间稍微延迟，避免过于频繁的API调用
                if (currentBatch < batchCount) {
                    Thread.sleep(100);
                }
                
            } catch (Exception e) {
                log.error("❌ 批次 {}/{} 存储失败: {}", currentBatch, batchCount, e.getMessage());
                throw new RuntimeException("向量存储失败", e);
            }
        }
        
        log.info("✅ 所有文档分批存储完成！总计: {} 个文档", totalDocuments);
    }
    
    /**
     * 递归获取仓库所有文件信息
     */
    private List<GitHubFile> getAllRepositoryFiles(String owner, String repository, String branch, String path) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s", 
                    owner, repository, path, branch);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(personalAccessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            List<GitHubFile> allFiles = new ArrayList<>();
            
            if (jsonNode.isArray()) {
                for (JsonNode fileNode : jsonNode) {
                    GitHubFile file = parseGitHubFile(fileNode);
                    allFiles.add(file);
                    
                    // 如果是目录，递归获取子文件
                    if ("dir".equals(file.getType())) {
                        log.debug("🔍 扫描目录: {}", file.getPath());
                        List<GitHubFile> subFiles = getAllRepositoryFiles(owner, repository, branch, file.getPath());
                        allFiles.addAll(subFiles);
                    }
                }
            }
            
            return allFiles;
            
        } catch (Exception e) {
            log.warn("❌ 获取目录文件失败: {}, 错误: {}", path, e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * 下载文件内容并创建Document
     */
    private Document downloadAndCreateDocument(String owner, String repository, String branch, 
                                             GitHubFile file, Path localCodePath) throws IOException {
        // 获取文件内容
        String content = getFileContent(owner, repository, branch, file.getPath());
        if (content == null || content.trim().isEmpty()) {
            log.debug("⚠️ 文件内容为空: {}", file.getPath());
            return null;
        }
        
        // 保存到本地文件系统
        Path localFilePath = localCodePath.resolve(file.getPath());
        Files.createDirectories(localFilePath.getParent());
        Files.writeString(localFilePath, content);
        
        log.debug("💾 本地保存: {} -> {}", file.getPath(), localFilePath);
        
        // 创建Document对象
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("repository", owner + "/" + repository);
        metadata.put("branch", branch);
        metadata.put("file_path", file.getPath());
        metadata.put("file_name", file.getName());
        metadata.put("file_type", getFileExtension(file.getName()));
        metadata.put("file_size", content.length());
        metadata.put("download_url", file.getDownloadUrl());
        metadata.put("local_path", localFilePath.toString());
        metadata.put("from_cache", false);
        
        return new Document(content, metadata);
    }
    
    /**
     * 获取文件内容
     */
    private String getFileContent(String owner, String repository, String branch, String path) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s", 
                    owner, repository, path, branch);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(personalAccessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            if (jsonNode.has("content") && jsonNode.has("encoding")) {
                String content = jsonNode.get("content").asText();
                String encoding = jsonNode.get("encoding").asText();
                
                if ("base64".equals(encoding)) {
                    byte[] decodedBytes = Base64.getDecoder().decode(content.replaceAll("\\s", ""));
                    return new String(decodedBytes);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("❌ 获取文件内容失败: {}, 错误: {}", path, e.getMessage());
            return null;
        }
    }
    
    /**
     * 处理文档：分割和关键词丰富
     */
    private List<Document> processDocuments(List<Document> documents, boolean enableSplitting, boolean enableEnrichment) {
        log.info("⚙️ 开始处理文档，原始文档数量: {}", documents.size());
        
        // 1. 文档分割
        List<Document> splitDocuments;
        if (enableSplitting) {
            log.info("✂️ 开始文档分割...");
            splitDocuments = myTokenTextSplitter.splitCustomized(documents);
            log.info("✂️ 文档分割完成，片段数量: {} (增长 {}%)", 
                    splitDocuments.size(), 
                    documents.size() > 0 ? (splitDocuments.size() * 100 / documents.size() - 100) : 0);
        } else {
            log.info("✂️ 文档分割已禁用，使用原始文档");
            splitDocuments = new ArrayList<>(documents);
        }
        
        // 2. 关键词丰富（批量处理以提高效率）
        if (enableEnrichment) {
            log.info("🔑 开始关键词丰富...");
            List<Document> enrichedDocuments = new ArrayList<>();
            int batchSize = 5; // 减少批量大小，避免超时
            int totalBatches = (splitDocuments.size() + batchSize - 1) / batchSize;
            int successfulBatches = 0;
            int failedBatches = 0;
            
            for (int i = 0; i < splitDocuments.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, splitDocuments.size());
                List<Document> batch = splitDocuments.subList(i, endIndex);
                int currentBatch = (i / batchSize) + 1;
                
                try {
                    log.debug("🔑 处理批次 {}/{}: {} 个文档", currentBatch, totalBatches, batch.size());
                    
                    // 增加超时保护
                    List<Document> enrichedBatch = enrichDocumentsWithTimeout(batch);
                    enrichedDocuments.addAll(enrichedBatch);
                    successfulBatches++;
                    
                    if (currentBatch % 5 == 0 || currentBatch == totalBatches) {
                        log.info("🔑 关键词丰富进度: {}/{} 批次 (成功: {}, 失败: {})", 
                                currentBatch, totalBatches, successfulBatches, failedBatches);
                    }
                    
                    // 批次间稍微延迟，减少API压力
                    if (currentBatch < totalBatches) {
                        Thread.sleep(200);
                    }
                    
                } catch (Exception e) {
                    failedBatches++;
                    log.warn("⚠️ 批次 {}/{} 关键词丰富失败，使用原始文档: {}", 
                            currentBatch, totalBatches, e.getMessage());
                    enrichedDocuments.addAll(batch);
                }
            }
            
            log.info("✅ 文档处理完成，最终文档数量: {} (成功批次: {}, 失败批次: {})", 
                    enrichedDocuments.size(), successfulBatches, failedBatches);
            return enrichedDocuments;
        } else {
            log.info("🔑 关键词丰富已禁用，使用分片后的文档");
            log.info("✅ 文档处理完成，最终文档数量: {}", splitDocuments.size());
            return splitDocuments;
        }
    }
    
    /**
     * 带超时保护的关键词丰富
     */
    private List<Document> enrichDocumentsWithTimeout(List<Document> documents) {
        try {
            // 使用CompletableFuture实现超时
            return java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> myKeywordEnricher.enrichDocuments(documents))
                    .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .get();
        } catch (java.util.concurrent.CompletionException e) {
            if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                log.warn("⏰ 关键词丰富超时，返回原始文档");
            } else {
                log.warn("❌ 关键词丰富异常: {}，返回原始文档", e.getMessage());
            }
            return documents;
        } catch (Exception e) {
            log.warn("❌ 关键词丰富异常: {}，返回原始文档", e.getMessage());
            return documents;
        }
    }
    
    /**
     * 创建本地存储目录
     */
    private Path createLocalDirectory(String repositoryName) throws IOException {
        Path localCodePath = Paths.get("tmp/code", repositoryName);
        Files.createDirectories(localCodePath);
        return localCodePath;
    }
    
    /**
     * 判断是否为代码文件
     */
    private boolean isCodeFile(String fileName) {
        String extension = getFileExtension(fileName);
        return CODE_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }
    
    /**
     * 解析GitHub文件信息
     */
    private GitHubFile parseGitHubFile(JsonNode fileNode) {
        GitHubFile file = new GitHubFile();
        file.setName(fileNode.get("name").asText());
        file.setPath(fileNode.get("path").asText());
        file.setType(fileNode.get("type").asText());
        if (fileNode.has("download_url") && !fileNode.get("download_url").isNull()) {
            file.setDownloadUrl(fileNode.get("download_url").asText());
        }
        return file;
    }
    
    /**
     * GitHub文件信息类
     */
    public static class GitHubFile {
        private String name;
        private String path;
        private String type;
        private String downloadUrl;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }
} 