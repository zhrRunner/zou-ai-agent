package wiki.zhr.zouaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
public class PgVectorVectorStoreConfigTest {

    @Resource(name = "pgVectorVectorStore")
    private VectorStore pgVectorVectorStore;

    @Test
    void pgVectorVectorStore() {
        List<Document> documents = List.of(
                new Document("zouhr宇宙无敌牛逼，from 中国科学院大学", Map.of("meta1", "meta1")),
                new Document("zouhr coding能力很强 大学时就很强", Map.of("meta1", "meta1")),
                new Document("长风破浪会有时，直挂云帆济沧海 是 我的座右铭", Map.of("meta2", "meta2")));
        // 添加文档
        pgVectorVectorStore.add(documents);
        // 相似度查询
        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("zouhr是哪个大学的").topK(5).build());
        Assertions.assertNotNull(results);
    }
}
