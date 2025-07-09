package wiki.zhr.zouaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CodeAssistantDocumentLoaderTest {
    @Resource
    private CodeAssistantDocumentLoader codeAssistantDocumentLoader;

    @Test
    void loadMarkdowns() {
        codeAssistantDocumentLoader.loadMarkdowns();
    }
}