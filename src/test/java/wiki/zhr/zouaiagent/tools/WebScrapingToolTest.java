package wiki.zhr.zouaiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class WebScrapingToolTest {

    @Test
    void scrapeWebPage() {
        WebScrapingTool tool = new WebScrapingTool();
        String url = "http://www.nssc.ac.cn/yjsb/zsxx/zsdt/202209/t20220929_6518747.html";
        String result = tool.scrapeWebPage(url);
        assertNotNull(result);
    }
}