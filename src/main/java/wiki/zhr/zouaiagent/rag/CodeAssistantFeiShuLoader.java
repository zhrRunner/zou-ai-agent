package wiki.zhr.zouaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import wiki.zhr.zouaiagent.reader.FeiShuDocumentReader;
import wiki.zhr.zouaiagent.reader.FeiShuResource;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class CodeAssistantFeiShuLoader {
    private FeiShuDocumentReader feiShuDocumentReader;

    private static final String FEISHU_APP_ID = System.getenv("FEISHU_APP_ID");

    private static final String FEISHU_APP_SECRET = System.getenv("FEISHU_APP_SECRET");

    private static final String FEISHU_DOCUMENT_ID = System.getenv("FEISHU_DOCUMENT_ID");

    private String FEISHU_USER_TOKEN;

    private FeiShuResource feiShuResource;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    public List<Document> loadFeiShuDocs() {
        try {
            FEISHU_USER_TOKEN = getFeiShuUserToken();
        } catch (IOException e) {
            log.error("Failed to read FeiShu user token", e);
            throw new RuntimeException("Failed to read FeiShu user token", e);
        }

        feiShuResource = FeiShuResource.builder()
                .appId(FEISHU_APP_ID)
                .appSecret(FEISHU_APP_SECRET)
                .build();

        feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource, FEISHU_USER_TOKEN, FEISHU_DOCUMENT_ID);
        List<Document> documentList = feiShuDocumentReader.get();

        // RAG优化
        // 使用切词器自主切分文档
        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documentList);

        // 自动补充关键词元信息
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(splitDocuments);

        log.info("enrichedDocuments:{}", enrichedDocuments);
        return enrichedDocuments;
    }

    private String getFeiShuUserToken() throws IOException {
        // 从tmp/feishu_user_token.txt文件中读取FEISHU_USER_TOKEN
        String token = FileUtils.readFileToString(new File(System.getProperty("user.dir") + "/tmp/feishu_user_token.txt"), "UTF-8");
        return token;
    }
}
