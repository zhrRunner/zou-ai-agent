package wiki.zhr.zouaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class CodeAssistantAppTest {

    @Resource
    private CodeAssistantApp codeAssistantApp;

    @Test
    void testdoChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是邹浩冉";
        String answer = codeAssistantApp.doChat(message, chatId);
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
        answer = codeAssistantApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我忘了我叫啥名字了，你帮我回忆一下";
        answer = codeAssistantApp.doChat(message, chatId);
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
        CodeAssistantApp.CodeAssistantReport codeAssistantReport = codeAssistantApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(codeAssistantReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "状态码为409的错误分支是什么？返回什么信息？";
        // String message = "zouhr是哪个大学的？"; // 测试PgVector
        String answer = codeAssistantApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        // testMessage("我想知道邹浩冉这个人的信息，你能在网上搜索一下吗？");

        // 测试网页抓取：恋爱案例分析
        // testMessage("最近对Java的基础有点遗忘了，你能帮我抓取一篇关于Java基础的文章吗？");

        // 测试资源下载：图片下载
        // testMessage("下载这个吧：https://www.runoob.com/java/java-tutorial.html");
        // testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");

        // 测试终端操作：执行代码
        // testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
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
        // testMessage(message + "保存我的代码审查报告为文件");

        // 测试 PDF 生成
        // testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");

        // 测试邮件发送
        testMessage("给438345469@qq.com发送邮件，主题为‘七夕约会计划’，内容为‘期待与你共度浪漫时光’，并附上本地文件路径为：" +
                "/Users/hrz/zou-ai-agent/zou-ai-agent/tmp/pdf/点我哟.pdf");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = codeAssistantApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试github MCP 获取zhrRunner的仓库列表信息
//        String message = "   请列出 https://github.com/zhrRunner 下的所有仓库";
        String message = " 获取 GitHub 用户 zhrRunner 的仓库 zou-ai-agent 的代码";
        String answer = codeAssistantApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
        Assertions.assertTrue(answer.contains("zhrRunner"));
    }

}
