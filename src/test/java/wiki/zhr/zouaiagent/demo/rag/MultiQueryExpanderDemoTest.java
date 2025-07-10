package wiki.zhr.zouaiagent.demo.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.boot.test.context.SpringBootTest;
import wiki.zhr.zouaiagent.demo.rag.MultiQueryExpanderDemo;

import java.util.List;import static org.junit.jupiter.api.Assertions .*;

@SpringBootTest
class MultiQueryExpanderDemoTest {
    @Resource private MultiQueryExpanderDemo multiQueryExpanderDemo;
    @Test void expand () {
        List<Query> queries = multiQueryExpanderDemo.expand("哈哈哈哈哈zouhr是哈哈哈哈哪个学校哈哈哈哈的？？？");
        Assertions.assertNotNull(queries);
    }
}
