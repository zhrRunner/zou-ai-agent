package wiki.zhr.zouaiagent.rag;


import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CodeAssistantVectorStoreConfig {

    @Resource
    private CodeAssistantDocumentLoader codeAssistantDocumentLoader;

    @Bean
    VectorStore CodeAssistantAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        // 加载markdown文档
//        List<Document> documents = codeAssistantDocumentLoader.loadMarkdowns();

        // 加载FeiShu文档
        List<Document> documents = new CodeAssistantFeiShuLoader().loadFeiShuDocs();
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }
}

