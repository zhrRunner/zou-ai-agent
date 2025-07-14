package wiki.zhr.zouaiagent.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * 动态PgVector存储服务，支持为不同的仓库创建独立的向量表
 */
@Service
@Slf4j
public class DynamicPgVectorStoreService {
    
    @Resource
    private JdbcTemplate jdbcTemplate;
    
    @Resource
    private EmbeddingModel dashscopeEmbeddingModel;
    
    // 缓存不同仓库的VectorStore实例
    private final ConcurrentHashMap<String, VectorStore> vectorStoreCache = new ConcurrentHashMap<>();
    
    /**
     * 获取或创建指定仓库的VectorStore
     * @param repositoryName 仓库名称，用作表名
     * @return VectorStore实例
     */
    public VectorStore getVectorStore(String repositoryName) {
        return vectorStoreCache.computeIfAbsent(repositoryName, this::createVectorStore);
    }
    
    /**
     * 创建新的VectorStore实例
     */
    private VectorStore createVectorStore(String repositoryName) {
        // 清理仓库名称，确保符合PostgreSQL表名规范
        String tableName = sanitizeTableName(repositoryName);
        String fullTableName = "code_" + tableName;
        
        log.info("🔧 创建向量存储: {}", fullTableName);
        
        // 检查表是否存在，如果不存在则手动创建
        ensureTableExists(fullTableName);
        
        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("zou_ai_agent")
                .vectorTableName(fullTableName)  // 添加前缀避免冲突
                .maxDocumentBatchSize(1000)  // 代码文件较小，减少批次大小
                .build();
    }
    
    /**
     * 确保表存在，如果不存在则创建
     */
    private void ensureTableExists(String tableName) {
        try {
            // 检查表是否存在
            String checkTableSql = 
                "SELECT EXISTS (" +
                "   SELECT FROM information_schema.tables " +
                "   WHERE table_schema = 'zou_ai_agent' " +
                "   AND table_name = ?" +
                ")";
            
            Boolean tableExists = jdbcTemplate.queryForObject(checkTableSql, Boolean.class, tableName);
            
            if (Boolean.TRUE.equals(tableExists)) {
                log.debug("✅ 表已存在: zou_ai_agent.{}", tableName);
                return;
            }
            
            log.info("🔨 创建向量表: zou_ai_agent.{}", tableName);
            
            // 创建表
            String createTableSql = String.format(
                "CREATE TABLE zou_ai_agent.%s (" +
                "    id uuid NOT NULL DEFAULT uuid_generate_v4()," +
                "    content text," +
                "    metadata json," +
                "    embedding vector(1536)" +
                ")", tableName);
            
            jdbcTemplate.execute(createTableSql);
            
            // 创建主键
            String createPrimaryKeySql = String.format(
                "ALTER TABLE zou_ai_agent.%s ADD CONSTRAINT %s_pkey PRIMARY KEY (id)",
                tableName, tableName);
            
            jdbcTemplate.execute(createPrimaryKeySql);
            
            // 创建向量索引
            String createIndexSql = String.format(
                "CREATE INDEX spring_ai_vector_index_%s ON zou_ai_agent.%s USING hnsw (embedding vector_cosine_ops)",
                tableName, tableName);
            
            jdbcTemplate.execute(createIndexSql);
            
            log.info("✅ 向量表创建成功: zou_ai_agent.{}", tableName);
            
        } catch (Exception e) {
            log.error("❌ 创建向量表失败: zou_ai_agent.{}, 错误: {}", tableName, e.getMessage());
            throw new RuntimeException("创建向量表失败", e);
        }
    }
    
    /**
     * 清理表名，确保符合PostgreSQL命名规范
     */
    private String sanitizeTableName(String repositoryName) {
        return repositoryName
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")  // 替换非法字符为下划线
                .replaceAll("_{2,}", "_")       // 合并多个连续下划线
                .replaceAll("^_|_$", "");       // 移除首尾下划线
    }
    
    /**
     * 清理指定仓库的向量数据
     */
    public void clearRepository(String repositoryName) {
        VectorStore vectorStore = vectorStoreCache.get(repositoryName);
        if (vectorStore instanceof PgVectorStore pgVectorStore) {
            // 清空表数据
            String tableName = "code_" + sanitizeTableName(repositoryName);
            try {
                jdbcTemplate.execute("TRUNCATE TABLE zou_ai_agent." + tableName);
                log.info("🧹 清理向量数据完成: {}", tableName);
            } catch (Exception e) {
                log.warn("⚠️ 清理向量数据失败: {}, 错误: {}", tableName, e.getMessage());
            }
        }
    }
    
    /**
     * 删除指定仓库的向量表
     */
    public void deleteRepository(String repositoryName) {
        String tableName = "code_" + sanitizeTableName(repositoryName);
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS zou_ai_agent." + tableName);
            vectorStoreCache.remove(repositoryName);
            log.info("🗑️ 删除向量表完成: {}", tableName);
        } catch (Exception e) {
            log.warn("⚠️ 删除向量表失败: {}, 错误: {}", tableName, e.getMessage());
        }
    }
} 