package wiki.zhr.zouaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebSearchToolTest {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Test
    void searchWeb() {

        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        String query = "邹浩冉";
        String result = webSearchTool.searchWeb(query);
        System.out.println(result);
        assertNotNull(result);
        assertTrue(result.contains("邹浩冉"));
    }
}