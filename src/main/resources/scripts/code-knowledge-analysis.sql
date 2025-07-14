-- ====================================================================
-- 代码知识库数据分析脚本
-- ====================================================================
-- 描述：用于查询和分析GitHub代码知识库的数据
-- 作者：GitHub代码知识库功能
-- 创建时间：2025-01-14
-- 用途：开发调试、数据分析、运维检查
-- ====================================================================

-- ====================================================================
-- 1. 查看所有代码知识库表
-- ====================================================================
-- 用于查看当前schema中所有以'code_'开头的表（代码知识库表）
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'zou_ai_agent'
  AND table_name LIKE 'code_%';

-- ====================================================================
-- 2. 统计文档数量
-- ====================================================================
-- 查看指定代码库的文档总数
-- 注意：替换表名为实际的代码库表名
SELECT COUNT(*) as document_count
FROM zou_ai_agent.code_zhrrunner_zou_ai_agent;

-- ====================================================================
-- 3. 查看文档内容预览
-- ====================================================================
-- 查看前30条文档的内容预览和元数据
-- content_preview显示前400个字符，便于快速查看文档内容
SELECT
    id,
    LEFT(content, 400) as content_preview,  -- 显示前400个字符
    metadata
FROM zou_ai_agent.code_zhrrunner_zou_ai_agent
LIMIT 30;

-- ====================================================================
-- 4. 文件类型分布分析
-- ====================================================================
-- 分析代码库中各种文件类型的分布情况
-- 帮助了解代码库的组成结构
SELECT
    metadata->>'file_type' as file_type,
    COUNT(*) as count
FROM zou_ai_agent.code_zhrrunner_zou_ai_agent
GROUP BY metadata->>'file_type'
ORDER BY count DESC;

-- ====================================================================
-- 5. 扩展查询（可选）
-- ====================================================================

-- 5.1 按文件路径查看文档分布
SELECT
    metadata->>'file_path' as file_path,
    metadata->>'file_name' as file_name,
    metadata->>'file_type' as file_type,
    LENGTH(content) as content_length
FROM zou_ai_agent.code_zhrrunner_zou_ai_agent
ORDER BY content_length DESC
LIMIT 20;

-- 5.2 查看特定文件类型的文档
-- 示例：查看所有Java文件
SELECT
    metadata->>'file_path' as file_path,
    LEFT(content, 200) as content_preview
FROM zou_ai_agent.code_zhrrunner_zou_ai_agent
WHERE metadata->>'file_type' = '.java'
ORDER BY metadata->>'file_path'
LIMIT 10;

-- 5.3 查看文档大小分布
SELECT
    CASE
        WHEN LENGTH(content) < 1000 THEN '小文件(<1KB)'
        WHEN LENGTH(content) < 5000 THEN '中等文件(1-5KB)'
        WHEN LENGTH(content) < 20000 THEN '大文件(5-20KB)'
        ELSE '超大文件(>20KB)'
    END as size_category,
    COUNT(*) as count,
    ROUND(AVG(LENGTH(content))) as avg_size
FROM zou_ai_agent.code_zhrrunner_zou_ai_agent
GROUP BY size_category
ORDER BY avg_size;

-- ====================================================================
-- 使用说明
-- ====================================================================
-- 1. 在使用前，请将 'code_zhrrunner_zou_ai_agent' 替换为实际的表名
-- 2. 可以根据需要修改LIMIT数量
-- 3. 对于大型代码库，建议先运行COUNT查询了解数据规模
-- 4. 如需查询特定仓库，请使用第一个查询找到正确的表名
-- ==================================================================== 