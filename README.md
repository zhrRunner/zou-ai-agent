# zouhr-ai-agent
这是一个关于AI超级智能体的项目
主要功能包括：
1. 帮助开发人员自动生成单元测试代码
2. 更好的做代码审查
3. MCP访问项目库代码 + 构建用户专属项目文档&技术方案RAG知识库 => 专属项目问答助手

## RAG
如果你想接入你的飞书文档作为知识库，暂时只支持一篇飞书文档
需要在飞书开放平台创建应用
然后你需要申请如下作为环境变量
FEISHU_APP_ID
FEISHU_APP_SECRET
FEISHU_DOCUMENT_ID

并且需要去获取FEISHU_USER_TOKEN
1. 启动项目
2. 在浏览器输入 https://accounts.feishu.cn/open-apis/authen/v1/authorize?client_id=cli_a8e76d199338900e&redirect_uri=http://localhost:8123/api/oauth/callback&scope=space:document:retrieve%20docx:document
3. 点击授权，token会保存在一个tmp/user_token.txt 文件中
暂未实现refresh token功能
其中可能对你有帮助的文档链接 
1. [飞书获取访问凭证](https://open.feishu.cn/document/server-docs/authentication-management/access-token/tenant_access_token_internal)
2. [可能遇到的issue](https://open.feishu.cn/document/faq/trouble-shooting/how-to-resolve-error-99991679)





