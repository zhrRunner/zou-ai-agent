package wiki.zhr.zouaiagent.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

@Service
public class GitHubService {
    @Value("${github.client_id}")
    private String clientId;
    @Value("${github.client_secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 用授权码换取access_token
     */
    public String getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";
        HttpHeaders headers = new HttpHeaders();
        // headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        // 简单处理，实际应解析JSON
        String body = response.getBody();
        if (body != null && body.contains("access_token")) {
            String[] parts = body.split("&");
            for (String part : parts) {
                if (part.startsWith("access_token=")) {
                    return part.split("=")[1];
                }
            }
        }
        return null;
    }

    /**
     * 拉取用户仓库代码（简单实现：下载zip包并解压）
     */
    public File downloadRepoZip(String owner, String repo, String accessToken) throws IOException {
        String url = String.format("https://api.github.com/repos/%s/%s/zipball", owner, repo);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        byte[] zipBytes = response.getBody();
        if (zipBytes == null)
            throw new IOException("下载失败");
        Path tempFile = Files.createTempFile(repo, ".zip");
        Files.write(tempFile, zipBytes);
        return tempFile.toFile();
    }
}