package wiki.zhr.zouaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 这是一个切词器，将List<Document>进行切分使得数量增加
 */
@Component
public class MyTokenTextSplitter {
    public List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    public List<Document> splitCustomized(List<Document> documents) {
        // 限制每个chunk最大1800字符，为DashScope embedding留出安全边界 (2048-248=1800)
        TokenTextSplitter splitter = new TokenTextSplitter(400, 100, 10, 1800, true);
        return splitter.apply(documents);
    }
}
