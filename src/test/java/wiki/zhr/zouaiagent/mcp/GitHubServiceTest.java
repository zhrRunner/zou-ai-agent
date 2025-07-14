package wiki.zhr.zouaiagent.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.File;

@SpringBootTest
public class GitHubServiceTest {
    @Autowired
    private GitHubService gitHubService;

    @Test
    public void testDownloadRepoZip() throws Exception {
        // 这里需要你手动填入有效的accessToken、owner和repo
        String accessToken = "";
        String owner = "zhrRunner";
        String repo = "zou-ai-agent";
        File zipFile = gitHubService.downloadRepoZip(owner, repo, accessToken);
        System.out.println("下载的zip文件路径: " + zipFile.getAbsolutePath());
        assert zipFile.exists();
    }
}