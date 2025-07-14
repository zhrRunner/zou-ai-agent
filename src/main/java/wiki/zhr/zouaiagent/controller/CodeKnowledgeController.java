package wiki.zhr.zouaiagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wiki.zhr.zouaiagent.service.DynamicPgVectorStoreService;
import wiki.zhr.zouaiagent.service.GitHubCodeKnowledgeService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 代码知识库管理Controller
 */
@RestController
@RequestMapping("/code-knowledge")
@Tag(name = "代码知识库管理", description = "GitHub代码知识库的构建、查询和管理")
@Slf4j
public class CodeKnowledgeController {
    
    @Resource
    private GitHubCodeKnowledgeService gitHubCodeKnowledgeService;
    
    @Resource
    private DynamicPgVectorStoreService dynamicPgVectorStoreService;
    
    /**
     * 构建GitHub仓库的代码知识库
     */
    @PostMapping("/build")
    @Operation(summary = "构建代码知识库", description = "下载指定GitHub仓库的代码并构建向量知识库")
    public ResponseEntity<Map<String, Object>> buildCodeKnowledge(
            @Parameter(description = "GitHub用户名或组织名", example = "zhrRunner")
            @RequestParam String owner,
            @Parameter(description = "仓库名称", example = "zou-ai-agent")
            @RequestParam String repository,
            @Parameter(description = "分支名称", example = "main")
            @RequestParam(defaultValue = "main") String branch,
            @Parameter(description = "是否异步执行", example = "true")
            @RequestParam(defaultValue = "true") boolean async,
            @Parameter(description = "是否启用文档分片（需要消耗token）", example = "true")
            @RequestParam(defaultValue = "true") boolean enableSplitting,
            @Parameter(description = "是否启用关键词丰富（需要消耗token）", example = "true")
            @RequestParam(defaultValue = "true") boolean enableEnrichment) {
        
        Map<String, Object> result = new HashMap<>();
        String repositoryName = owner + "_" + repository;
        
        try {
            if (async) {
                // 异步执行构建
                CompletableFuture.runAsync(() -> {
                    try {
                        gitHubCodeKnowledgeService.buildCodeKnowledge(owner, repository, branch, enableSplitting, enableEnrichment);
                        log.info("异步构建代码知识库完成: {}/{}", owner, repository);
                    } catch (Exception e) {
                        log.error("异步构建代码知识库失败: {}/{}", owner, repository, e);
                    }
                });
                
                result.put("status", "accepted");
                result.put("message", "代码知识库构建任务已提交，正在后台执行");
                result.put("repository", repositoryName);
                result.put("options", Map.of(
                    "enableSplitting", enableSplitting,
                    "enableEnrichment", enableEnrichment,
                    "branch", branch
                ));
                return ResponseEntity.accepted().body(result);
            } else {
                // 同步执行构建
                gitHubCodeKnowledgeService.buildCodeKnowledge(owner, repository, branch, enableSplitting, enableEnrichment);
                result.put("status", "success");
                result.put("message", "代码知识库构建完成");
                result.put("repository", repositoryName);
                result.put("options", Map.of(
                    "enableSplitting", enableSplitting,
                    "enableEnrichment", enableEnrichment,
                    "branch", branch
                ));
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            log.error("构建代码知识库失败: {}/{}", owner, repository, e);
            result.put("status", "error");
            result.put("message", "构建失败: " + e.getMessage());
            result.put("repository", repositoryName);
            result.put("options", Map.of(
                "enableSplitting", enableSplitting,
                "enableEnrichment", enableEnrichment,
                "branch", branch
            ));
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 搜索代码知识库
     */
    @GetMapping("/search")
    @Operation(summary = "搜索代码知识库", description = "在指定仓库的代码知识库中搜索相关内容")
    public ResponseEntity<Map<String, Object>> searchCodeKnowledge(
            @Parameter(description = "GitHub用户名或组织名", example = "zhrRunner")
            @RequestParam String owner,
            @Parameter(description = "仓库名称", example = "zou-ai-agent")
            @RequestParam String repository,
            @Parameter(description = "搜索查询", example = "Spring Boot配置")
            @RequestParam String query,
            @Parameter(description = "返回结果数量", example = "5")
            @RequestParam(defaultValue = "5") int topK,
            @Parameter(description = "相似度阈值", example = "0.7")
            @RequestParam(defaultValue = "0.7") double threshold) {
        
        Map<String, Object> result = new HashMap<>();
        String repositoryName = owner + "_" + repository;
        
        try {
            VectorStore vectorStore = dynamicPgVectorStoreService.getVectorStore(repositoryName);
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(threshold)
                    .build();
            
            List<Document> searchResults = vectorStore.similaritySearch(searchRequest);
            
            result.put("status", "success");
            result.put("repository", repositoryName);
            result.put("query", query);
            result.put("total_results", searchResults.size());
            result.put("results", searchResults.stream().map(doc -> {
                Map<String, Object> docInfo = new HashMap<>();
                docInfo.put("content", doc.getText());
                docInfo.put("metadata", doc.getMetadata());
                return docInfo;
            }).toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("搜索代码知识库失败: {}/{}, 查询: {}", owner, repository, query, e);
            result.put("status", "error");
            result.put("message", "搜索失败: " + e.getMessage());
            result.put("repository", repositoryName);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 清理代码知识库
     */
    @DeleteMapping("/clear")
    @Operation(summary = "清理代码知识库", description = "清空指定仓库的代码知识库数据")
    public ResponseEntity<Map<String, Object>> clearCodeKnowledge(
            @Parameter(description = "GitHub用户名或组织名", example = "zhrRunner")
            @RequestParam String owner,
            @Parameter(description = "仓库名称", example = "zou-ai-agent")
            @RequestParam String repository) {
        
        Map<String, Object> result = new HashMap<>();
        String repositoryName = owner + "_" + repository;
        
        try {
            dynamicPgVectorStoreService.clearRepository(repositoryName);
            result.put("status", "success");
            result.put("message", "代码知识库已清理");
            result.put("repository", repositoryName);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("清理代码知识库失败: {}/{}", owner, repository, e);
            result.put("status", "error");
            result.put("message", "清理失败: " + e.getMessage());
            result.put("repository", repositoryName);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 删除代码知识库
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除代码知识库", description = "完全删除指定仓库的代码知识库（包括数据表）")
    public ResponseEntity<Map<String, Object>> deleteCodeKnowledge(
            @Parameter(description = "GitHub用户名或组织名", example = "zhrRunner")
            @RequestParam String owner,
            @Parameter(description = "仓库名称", example = "zou-ai-agent")
            @RequestParam String repository) {
        
        Map<String, Object> result = new HashMap<>();
        String repositoryName = owner + "_" + repository;
        
        try {
            dynamicPgVectorStoreService.deleteRepository(repositoryName);
            result.put("status", "success");
            result.put("message", "代码知识库已删除");
            result.put("repository", repositoryName);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("删除代码知识库失败: {}/{}", owner, repository, e);
            result.put("status", "error");
            result.put("message", "删除失败: " + e.getMessage());
            result.put("repository", repositoryName);
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查代码知识库服务状态")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "healthy");
        result.put("service", "code-knowledge");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }
} 