package wiki.zhr.zouaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ResourceDownloadToolTest {

    @Test
    void downloadResource() {
        ResourceDownloadTool downloadTool = new ResourceDownloadTool();
        String url = "https://zhr-blog.oss-cn-beijing.aliyuncs.com/blog/202311121038272.png";
        String result = downloadTool.downloadResource(url, "jujingyi.png");
        assertNotNull(result);
    }
}