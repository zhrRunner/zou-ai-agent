package wiki.zhr.zouaiagent.demo.invoke;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @ClassName SpringAiInvoke
 * @Description Spring AI框架调用大模型
 * @Author hrz
 * @Date 2025/6/29 18:24
 **/
//@Component
public class SpringAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashscopeChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage output = dashscopeChatModel.call(new Prompt("hello, 我是邹浩冉，长风破浪会有时的下一句是什么？"))
                .getResult()
                .getOutput();
        System.out.println(output.getText());
    }
}
