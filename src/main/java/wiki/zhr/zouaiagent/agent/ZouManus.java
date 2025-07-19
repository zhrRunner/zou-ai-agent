package wiki.zhr.zouaiagent.agent;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import wiki.zhr.zouaiagent.advisor.MyLoggerAdvisor;
import wiki.zhr.zouaiagent.service.DynamicPgVectorStoreService;

@Component
public class ZouManus extends ToolCallAgent {

    private final String repositoryName = "zhrRunner_zou-ai-agent";
    private final VectorStore gitHubVectorStore;
    private final ChatClient chatClient;

    public ZouManus(ToolCallback[] allTools,
                    ChatModel dashscopeChatModel,
                    DynamicPgVectorStoreService dynamicPgVectorStoreService) {
        super(allTools);
        this.setName("zouManus");
        String SYSTEM_PROMPT = """
                You are ZouManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);

        // 初始化gitHubVectorStore
        this.gitHubVectorStore = dynamicPgVectorStoreService.getVectorStore(repositoryName);

        // 初始化客户端
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor(), new QuestionAnswerAdvisor(gitHubVectorStore))
                .build();
        this.setChatClient(chatClient);
    }
}
