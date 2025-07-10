package wiki.zhr.zouaiagent.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpSession;
import wiki.zhr.zouaiagent.rag.CodeAssistantFeiShuLoader;
import wiki.zhr.zouaiagent.reader.FeiShuConfig;
import wiki.zhr.zouaiagent.reader.FeiShuDocumentReader;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/oauth")
@Slf4j
public class OAuthController {

    @Resource
    FeiShuConfig feiShuConfig;

    @Resource
    CodeAssistantFeiShuLoader codeAssistantFeiShuLoader;

    @Resource(name = "pgVectorVectorStore")
    private VectorStore pgVectorVectorStore;

    private String appId = System.getenv("FEISHU_APP_ID");

    private String appSecret = System.getenv("FEISHU_APP_SECRET");


    // 飞书 OAuth 回调接口
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpSession session) {

        System.out.println("Code: " + code);
        // 1. 用 code 兑换 user_access_token
        String userToken = fetchUserToken(code);

        // 2. 将 Token 存储到 Session
        session.setAttribute("feishu_user_token", userToken);

        // 3. 根据获得的token，将加载到的feishu文档写入PgVector
        // TODO 待优化，这里采用加载后入库，后续需要用户输入飞书配置信息，并同意是否存储到 PgVector 向量数据库，
        // 还需提供用户对该数据库的CRUD功能
        List<Document> documentList = codeAssistantFeiShuLoader.loadFeiShuDocs();
        // 写入PgVectorVectorStore
        pgVectorVectorStore.add(documentList);
        log.info("写入PgVector向量数据库 dockmentList:{}", documentList);


        // 4. 返回成功响应（可自定义跳转前端页面）
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Authorization successful</title>
                    </head>
                    <body>
                        <h1>Lark Authorization successful</h1>
                        <p>Token has been saved. You can close this page now.</p>
                        <script>
                            // 可选：通知父窗口刷新
                            window.opener.postMessage('auth-success', '*');
                            window.close();
                        </script>
                    </body>
                    </html>
                    """);
    }

    // 兑换 user_access_token 的私有方法
    private String fetchUserToken(String code) {
        String url = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 构建请求体
        String requestBody = String.format("""
            {
                "grant_type": "authorization_code",
                "code": "%s",
                "client_id": "%s",
                "client_secret": "%s",
                "redirect_uri": "http://localhost:8123/api/oauth/callback"
            }
            """, code, appId, appSecret);

        System.out.println("Request Body: " + requestBody);

        // 发送请求
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                new HttpEntity<>(requestBody, headers),
                Map.class);

        // 打印完整响应
        System.out.println("飞书响应完整内容: " + response.getBody());

        if (response.getBody() == null || !response.getBody().containsKey("access_token")) {
            // 如果响应体为空或不包含 access_token，抛出异常
            System.err.println("飞书返回错误: " + response.getBody());
            // 如果飞书返回错误详情，打印出来
            if (response.getBody() != null) {
                throw new RuntimeException("飞书错误: " + response.getBody().toString());
            }
            throw new RuntimeException("飞书返回空响应");
        }
        String accessToken = response.getBody().get("access_token").toString();
        System.out.println("Access Token: " + accessToken);
        feiShuConfig.setFEISHU_USER_TOKEN(accessToken);
        // 将 access_token 写入到tmp/feishu_user_token.txt文件中
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Paths.get("tmp/feishu_user_token.txt"),
                    accessToken
            );
            System.out.println("Access Token 已写入到文件: tmp/feishu_user_token.txt");
        } catch (java.io.IOException e) {
            System.err.println("写入 access_token 到文件失败: " + e.getMessage());
        }
        return accessToken;
    }
}