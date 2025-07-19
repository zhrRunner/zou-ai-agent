package wiki.zhr.zouaiagent.controller;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import wiki.zhr.zouaiagent.agent.ZouManus;
import wiki.zhr.zouaiagent.app.CodeAssistantApp;
import wiki.zhr.zouaiagent.service.DynamicPgVectorStoreService;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private CodeAssistantApp codeAssistantApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步方式与 Code Assistant 进行对话
     */
    @GetMapping("/codeAssistant/chat/sync")
    public String doChatWithCodeAssistantSync(String message, String chatId) {
        return codeAssistantApp.doChat(message, chatId);
    }

    /**
     * SSE流式与 Code Assistant 进行对话
     */
    @GetMapping(value = "/codeAssistant/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithCodeAssistantSSE(String message, String chatId) {
        return codeAssistantApp.doChatByStream(message, chatId);
    }

    @Resource
    private DynamicPgVectorStoreService dynamicPgVectorStoreService;

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        ZouManus zouManus = new ZouManus(allTools, dashscopeChatModel, dynamicPgVectorStoreService);
        return zouManus.runStream(message);
    }




}
