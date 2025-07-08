package wiki.zhr.zouaiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

/**
 * 使用 Hutool Http 工具封装阿里云 DashScope 文本生成接口的简单调用示例。
 * <p>
 * 将如下 cURL 请求转化为 Java 代码：
 * 
 * <pre>
 * curl --location "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation" \
 * --header "Authorization: Bearer sk-xxx" \
 * --header "Content-Type: application/json" \
 * --data '{
 *   "model": "qwen-plus",
 *   "input": {
 *     "messages": [
 *       {"role": "system", "content": "You are a helpful assistant."},
 *       {"role": "user", "content": "你好，我是邹浩冉，你是谁？"}
 *     ]
 *   },
 *   "parameters": {"result_format": "message"}
 * }'
 * </pre>
 * 
 * 依赖环境：
 * 
 * <pre>
 * cn.hutool:hutool-all
 * </pre>
 */
/*
 * @Author Zou hr
 * @Description HTTP远程调用大模型
 * @Date 18:25 2025/6/29
 * @Param
 * @return
 **/
public class HttpAiInvoke {

    /**
     * 调用文本生成接口并返回响应字符串。
     */
    public static String callWithHutool() {
        // 1. 请求地址
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        // 2. 构造请求体 JSON
        JSONObject bodyJson = new JSONObject();
        bodyJson.set("model", "qwen-plus");

        // 2.1 messages 数组
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", "You are a helpful assistant."));
        messages.add(new JSONObject().set("role", "user").set("content", "你好，我是邹浩冉，你是谁？"));

        // 2.2 input 对象
        JSONObject input = new JSONObject();
        input.set("messages", messages);
        bodyJson.set("input", input);

        // 2.3 parameters 对象
        JSONObject parameters = new JSONObject();
        parameters.set("result_format", "message");
        bodyJson.set("parameters", parameters);

        // 3. 发送 POST 请求
        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + TestApiKey.API_KEY) // 鉴权
                .header("Content-Type", "application/json")
                .body(bodyJson.toString())
                .timeout(30_000) // 可根据需要调节超时
                .execute();

        // 4. 返回接口响应
        return response.body();
    }

    /**
     * 演示入口：直接运行即可查看接口响应。
     */
    public static void main(String[] args) {
        String result = callWithHutool();
        System.out.println(result);
        // 若需更完善的异常处理，可结合 try-catch 并使用日志框架记录。
    }
}