# GitHub代码知识库使用指南

## 功能概述
本功能可以将GitHub仓库的代码下载到本地，转换为Spring AI的Document对象，并存储到pgVector向量数据库中，构建代码知识库以支持RAG功能。

## 特性
- ✅ 支持任意公开GitHub仓库
- ✅ 自动递归下载所有代码文件
- ✅ 智能过滤代码文件类型（支持30+编程语言）
- ✅ 文档自动分割和关键词丰富
- ✅ 为每个仓库创建独立的向量表
- ✅ 支持异步构建，不阻塞API响应
- ✅ 完整的RESTful API接口
- ✅ 本地文件系统持久化存储
- ✅ 可选择性启用文档处理功能，节省token消耗

## API接口

### 1. 构建代码知识库
```http
POST /api/code-knowledge/build
```

**参数：**
- `owner` (必需): GitHub用户名或组织名，如 `zhrRunner`
- `repository` (必需): 仓库名称，如 `zou-ai-agent`
- `branch` (可选): 分支名称，默认 `main`
- `async` (可选): 是否异步执行，默认 `true`
- `enableSplitting` (可选): 是否启用文档分片，默认 `true` ⚠️ **需要消耗token**
- `enableEnrichment` (可选): 是否启用关键词丰富，默认 `true` ⚠️ **需要消耗token**

#### 💰 Token消耗说明
- **文档分片（enableSplitting）**: 不直接消耗token，但会增加文档数量，间接影响后续processing
- **关键词丰富（enableEnrichment）**: **大量消耗token**，会为每个文档片段调用ChatModel生成关键词

#### 🎯 使用建议
- **测试阶段**: 建议设置 `enableSplitting=false&enableEnrichment=false` 节省token
- **生产环境**: 根据预算和需求灵活调整
- **小型项目**: 可以全部启用以获得最佳搜索效果
- **大型项目**: 建议先关闭enrichment，评估效果后再决定

**示例：**

**完整功能（消耗token）：**
```bash
curl -X POST "http://localhost:8123/api/code-knowledge/build" \
  -d "owner=zhrRunner" \
  -d "repository=zou-ai-agent" \
  -d "branch=main" \
  -d "async=true" \
  -d "enableSplitting=true" \
  -d "enableEnrichment=true"
```

**节省token模式：**
```bash
curl -X POST "http://localhost:8123/api/code-knowledge/build" \
  -d "owner=zhrRunner" \
  -d "repository=zou-ai-agent" \
  -d "branch=main" \
  -d "async=true" \
  -d "enableSplitting=false" \
  -d "enableEnrichment=false"
```

**仅分片模式（推荐）：**
```bash
curl -X POST "http://localhost:8123/api/code-knowledge/build" \
  -d "owner=zhrRunner" \
  -d "repository=zou-ai-agent" \
  -d "branch=main" \
  -d "async=true" \
  -d "enableSplitting=true" \
  -d "enableEnrichment=false"
```

**响应：**
```json
{
  "status": "accepted",
  "message": "代码知识库构建任务已提交，正在后台执行",
  "repository": "zhrRunner_zou-ai-agent",
  "options": {
    "enableSplitting": true,
    "enableEnrichment": false,
    "branch": "main"
  }
}
```

### 2. 搜索代码知识库
```http
GET /api/code-knowledge/search
```

**参数：**
- `owner` (必需): GitHub用户名或组织名
- `repository` (必需): 仓库名称
- `query` (必需): 搜索查询，如 `Spring Boot配置`
- `topK` (可选): 返回结果数量，默认 `5`
- `threshold` (可选): 相似度阈值，默认 `0.7`

**示例：**
```bash
curl -X GET "http://localhost:8123/api/code-knowledge/search" \
  -d "owner=zhrRunner" \
  -d "repository=zou-ai-agent" \
  -d "query=Spring Boot配置" \
  -d "topK=3"
```

**响应：**
```json
{
  "status": "success",
  "repository": "zhrRunner_zou-ai-agent",
  "query": "Spring Boot配置",
  "total_results": 3,
  "results": [
    {
      "content": "spring:\n  application:\n    name: zou-ai-agent...",
      "metadata": {
        "repository": "zhrRunner/zou-ai-agent",
        "file_path": "src/main/resources/application.yml",
        "file_name": "application.yml",
        "file_type": ".yml",
        "branch": "main"
      }
    }
  ]
}
```

### 3. 清理代码知识库
```http
DELETE /api/code-knowledge/clear
```

清空指定仓库的向量数据，但保留表结构。

### 4. 删除代码知识库
```http
DELETE /api/code-knowledge/delete
```

完全删除指定仓库的向量表和所有数据。

### 5. 健康检查
```http
GET /api/code-knowledge/health
```

## 使用示例

### 构建您自己仓库的代码知识库
```bash
# 1. 构建代码知识库
curl -X POST "http://localhost:8123/api/code-knowledge/build" \
  -d "owner=zhrRunner" \
  -d "repository=zou-ai-agent" \
  -d "async=true"

# 2. 等待构建完成（查看日志）

# 3. 搜索代码
curl -X GET "http://localhost:8123/api/code-knowledge/search" \
  -d "owner=zhrRunner" \
  -d "repository=zou-ai-agent" \
  -d "query=RAG相关代码" \
  -d "topK=5"
```

### 支持的编程语言
- **后端**: `.java`, `.py`, `.js`, `.ts`, `.cpp`, `.c`, `.h`, `.cs`, `.go`, `.php`, `.rb`, `.swift`, `.kt`, `.scala`, `.rs`
- **前端**: `.vue`, `.jsx`, `.tsx`, `.html`, `.css`, `.scss`, `.less`
- **配置**: `.sql`, `.xml`, `.yaml`, `.yml`, `.json`, `.properties`
- **脚本**: `.sh`, `.bat`
- **文档**: `.md`, `.txt`

## 数据存储

### 本地文件存储
代码文件下载到：`/tmp/code/{owner}_{repository}/`

### 数据库存储
- **Schema**: `zou_ai_agent`
- **表名**: `code_{owner}_{repository}` (自动清理特殊字符)
- **向量维度**: 1536 (DashScope embedding)
- **索引类型**: HNSW
- **距离类型**: COSINE_DISTANCE

### 文档元数据
每个代码片段包含以下元数据：
```json
{
  "repository": "owner/repository",
  "branch": "main",
  "file_path": "src/main/java/Package.java",
  "file_name": "Package.java",
  "file_type": ".java",
  "file_size": 1234,
  "download_url": "https://raw.githubusercontent.com/...",
  "local_path": "/tmp/code/owner_repo/src/main/java/Package.java"
}
```

## 配置要求

### 1. GitHub Personal Access Token
在 `application-local.yml` 中配置：
```yaml
github:
  personal_access_token: your_github_token_here
```

### 2. pgVector数据库
确保已安装并配置pgVector扩展：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/zou_ai_agent
    username: my_user
    password: your_password
```

### 3. DashScope API Key
配置阿里云DashScope API Key用于文本嵌入：
```yaml
spring:
  ai:
    dashscope:
      api-key: your_dashscope_api_key
```

## 最佳实践

### 1. Token成本优化 💰
```bash
# 🥇 推荐配置：仅启用分片，关闭关键词丰富
curl -X POST "..." \
  -d "enableSplitting=true" \
  -d "enableEnrichment=false"

# 🥈 测试配置：都关闭，快速测试
curl -X POST "..." \
  -d "enableSplitting=false" \
  -d "enableEnrichment=false"

# 🥉 完整配置：功能最全，但消耗最多token
curl -X POST "..." \
  -d "enableSplitting=true" \
  -d "enableEnrichment=true"
```

**Token消耗对比：**
- **关闭所有**: 几乎不消耗token，仅embedding消耗
- **仅分片**: 轻微增加embedding消耗（文档数量增加）
- **仅丰富**: 中等消耗，为每个文档生成关键词
- **全部启用**: 最高消耗，分片+关键词丰富

### 2. 异步构建
对于大型仓库，建议使用异步构建：
```bash
curl -X POST "..." -d "async=true"
```

### 3. 分批搜索
如果搜索结果太多，可以调整参数：
- 增加 `threshold` 提高相似度要求
- 减少 `topK` 限制结果数量

### 4. 定期清理
对于经常更新的仓库，建议定期重新构建：
```bash
# 清理旧数据
curl -X DELETE "..." 

# 重新构建
curl -X POST "..." 
```

### 5. 监控日志
构建过程中关注应用日志：
```
[INFO] 🚀 开始构建代码知识库: zhrRunner/zou-ai-agent (branch: main, 分片: true, 丰富: false)
[INFO] 📥 文件下载完成！成功: 45, 跳过缓存: 0, 失败: 0, 总计: 45
[INFO] ✂️ 文档分割完成，片段数量: 123 (增长 173%)
[INFO] 🔑 关键词丰富已禁用，使用分片后的文档
[INFO] 💾 开始分批存储，总文档: 123, 批次大小: 20, 总批次: 7
[INFO] ✅ 代码知识库构建完成: zhrRunner/zou-ai-agent, 耗时: 45232ms, 共处理 123 个文档片段
```

## 故障排除

### 1. GitHub API限制
- 确保Personal Access Token有效
- 注意GitHub API速率限制（每小时5000次请求）
- 对于私有仓库，确保token有相应权限

### 2. 数据库连接
- 检查pgVector扩展是否正确安装
- 确认数据库连接配置正确
- 验证用户权限

### 3. 大文件处理
- 系统会自动跳过过大的文件
- 二进制文件会被忽略
- 如果遇到内存问题，可以调整JVM参数

---

通过这个功能，您可以轻松地将任何GitHub仓库转换为可搜索的代码知识库，为您的AI助手提供强大的代码理解和问答能力！

## 快速测试脚本

为了帮助您快速验证功能，这里提供一些测试脚本：

### 🧪 测试脚本1：节省token的快速验证
```bash
#!/bin/bash

# 基础配置
BASE_URL="http://localhost:8123/api/code-knowledge"
OWNER="zhrRunner"
REPO="zou-ai-agent"

echo "🚀 开始构建代码知识库（节省token模式）..."

# 构建知识库（关闭所有token消耗功能）
curl -X POST "${BASE_URL}/build" \
  -d "owner=${OWNER}" \
  -d "repository=${REPO}" \
  -d "branch=main" \
  -d "async=false" \
  -d "enableSplitting=false" \
  -d "enableEnrichment=false"

echo -e "\n⏰ 等待5秒..."
sleep 5

echo "🔍 测试搜索功能..."
# 搜索测试
curl -X GET "${BASE_URL}/search" \
  -d "owner=${OWNER}" \
  -d "repository=${REPO}" \
  -d "query=Spring Boot" \
  -d "topK=2"

echo -e "\n✅ 测试完成！"
```

### 🔥 测试脚本2：推荐配置（仅分片）
```bash
#!/bin/bash

BASE_URL="http://localhost:8123/api/code-knowledge"
OWNER="zhrRunner"
REPO="zou-ai-agent"

echo "🚀 开始构建代码知识库（推荐配置）..."

# 构建知识库（仅启用分片）
curl -X POST "${BASE_URL}/build" \
  -d "owner=${OWNER}" \
  -d "repository=${REPO}" \
  -d "branch=main" \
  -d "async=true" \
  -d "enableSplitting=true" \
  -d "enableEnrichment=false"

echo -e "\n📋 任务已提交，请查看日志等待完成..."
echo "💡 完成后可以使用以下命令搜索："
echo "curl -X GET '${BASE_URL}/search' -d 'owner=${OWNER}' -d 'repository=${REPO}' -d 'query=你的搜索内容'"
```

### 🧹 清理脚本
```bash
#!/bin/bash

BASE_URL="http://localhost:8123/api/code-knowledge"
OWNER="zhrRunner"
REPO="zou-ai-agent"

echo "🧹 清理代码知识库..."

# 删除知识库
curl -X DELETE "${BASE_URL}/delete" \
  -d "owner=${OWNER}" \
  -d "repository=${REPO}"

echo -e "\n✅ 清理完成！"
```

### 📊 健康检查
```bash
#!/bin/bash

BASE_URL="http://localhost:8123/api/code-knowledge"

echo "🔍 检查服务状态..."
curl -X GET "${BASE_URL}/health"
echo -e "\n"
```

**使用方法：**
1. 将脚本保存为 `.sh` 文件
2. 给脚本执行权限：`chmod +x script_name.sh`
3. 运行：`./script_name.sh`

**注意事项：**
- 确保应用程序已启动（端口8123）
- 确保GitHub token配置正确
- 首次运行时，请先运行健康检查脚本 