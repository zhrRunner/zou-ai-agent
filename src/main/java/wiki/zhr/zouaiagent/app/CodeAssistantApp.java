package wiki.zhr.zouaiagent.app;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import wiki.zhr.zouaiagent.advisor.MyLoggerAdvisor;
import wiki.zhr.zouaiagent.chatmemory.FileBasedChatMemory;
import wiki.zhr.zouaiagent.rag.QueryRewriter;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @ClassName CodeAssistant
 * @Description 代码助手
 * @Author hrz
 * @Date 2025/7/5 14:22
 **/
@Component
@Slf4j
public class CodeAssistantApp {

    private final ChatClient chatClient;

    private final String SYSTEM_PROMPT = "你是一个经验丰富的资深软件工程师、自动化测试专家和代码审查顾问。你的职责是：\n" +
            "根据用户提供的业务代码片段，自动生成高质量、覆盖率高、结构清晰的单元测试代码。\n" +
            "对业务代码进行全面代码审查，识别潜在缺陷、逻辑漏洞、边界条件遗漏、安全问题和维护风险，输出清晰的审查报告。\n" +
            "在对话过程中不断主动提问以补齐上下文，深入了解用户代码所在的业务场景、依赖环境、项目结构、技术栈、代码约定和测试需求。\n" +
            "根据用户的反馈和补充信息，迭代优化测试代码和审查结论，确保测试具备**真实场景模拟、边界值测试、异常处理验证和依赖隔离（Mock）**能力。\n" +
            "严格区分单元测试与集成测试场景，优先生成纯粹的单元测试（mock 外部依赖），仅在明确需求时提供集成级别测试。\n" +
            "输出测试代码时，务必：\n" +
            "指明使用的测试框架（如 pytest、unittest、Jest、JUnit、Go test 等）\n" +
            "告知是否可直接运行，是否需要额外依赖或环境\n" +
            "提供注释或结构说明（如 Arrange-Act-Assert 分区）\n" +
            "输出审查报告时，务必包含：\n" +
            "风险等级（高 / 中 / 低）\n" +
            "问题分类（可读性、健壮性、安全性、性能等）\n" +
            "修复建议\n" +
            "请始终以专业、实用的方式回复用户，不要生成冗余内容或过度解释。如你无法明确判断某个上下文信息，请向用户提问澄清，而不是臆测。\n" +
            "你的目标是：帮助用户产出可靠、易维护、风险低的单元测试，并提升其代码质量和工程稳定性。";

    public CodeAssistantApp(ChatModel dashscopeChatModel) {
        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
//        // 初始化基于内存的对话记忆
//        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory)
                        // 自定义日志拦截器，可以按需开启
//                        ,new MyLoggerAdvisor()
//                        , new ReReadingAdvisor()
                )
                .build();
    }


    /**
     * 执行聊天
     *
     * @param message 用户输入的消息
     * @param chatId  聊天会话ID，用于标识对话上下文
     * @return AI助手的回复内容
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    record CodeAssistantReport(String title, List<String> suggestions) {

    }

    public CodeAssistantReport doChatWithReport(String message, String chatId) {
        CodeAssistantReport codeAssistantReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "对于单元测试生成和代码审查，都要生成代码建议报告，标题为代码审查报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(CodeAssistantReport.class);
        log.info("loveReport: {}", codeAssistantReport);
        return codeAssistantReport;
    }

    // AI 代码助手知识库问答
    @Resource
    @Qualifier("CodeAssistantAppVectorStore")
    private VectorStore codeAssistantAppVectorStore;

    @Resource(name = "pgVectorVectorStore")
    private VectorStore pgVectorVectorStore;


    @Resource
    private Advisor codeAssistantRagCloudAdvisor;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 和 RAG 知识库进行对话
     */
    public String doChatWithRag(String message, String chatId) {
        // 使用 QueryRewriter 对用户输入进行重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        log.info("rewrittenMessage: {}", rewrittenMessage);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用知识库问答 （可选飞书获取文档 \ 获取md文档）
                .advisors(new QuestionAnswerAdvisor(codeAssistantAppVectorStore))
                // 应用增强检索服务（云知识库服务————百炼）
//                .advisors(codeAssistantRagCloudAdvisor)
                // 应用RAG 检索增强服务（基于 PgVector 的向量存储————云数据库）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    /**
     * 代码助手工具调用 Tool Calling
     */
    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}


