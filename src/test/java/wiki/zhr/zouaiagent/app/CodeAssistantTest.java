package wiki.zhr.zouaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CodeAssistantTest {

    @Resource
    private CodeAssistant codeAssistant;

    @Test
    void testdoChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是邹浩冉";
        String answer = codeAssistant.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "给你这段代码，请你帮我生成单元测试 & 做代码审查" +
                "public long getCreateTimeDate(){\n" +
                "        if (StringUtils.isEmpty(this.createTime)) {\n" +
                "            return System.currentTimeMillis();\n" +
                "        }\n" +
                "        try {\n" +
                "            return LocalDateTime.parse(this.createTime, FORMATTER)\n" +
                "                    .atZone(ZoneId.systemDefault())\n" +
                "                    .toInstant()\n" +
                "                    .toEpochMilli();\n" +
                "        } catch (Exception e) {\n" +
                "            // 解析失败，返回当前时间\n" +
                "            return System.currentTimeMillis();\n" +
                "        }\n" +
                "    }";
        answer = codeAssistant.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我忘了我叫啥名字了，你帮我回忆一下";
        answer = codeAssistant.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = """
        请分析以下代码：
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(\\"yyyy-MM-dd HH:mm:ss.SSS\\");
        public long getCreateTimeDate()\\{
            if (StringUtils.isEmpty(this.createTime)) \\{
                return System.currentTimeMillis();
            \\}
            try \\{
                return LocalDateTime.parse(this.createTime, FORMATTER)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
            \\} catch (Exception e) \\{
                return System.currentTimeMillis();
            \\}
        \\}
        """;
        CodeAssistant.CodeAssistantReport codeAssistantReport = codeAssistant.doChatWithReport(message, chatId);
        Assertions.assertNotNull(codeAssistantReport);
    }
}
