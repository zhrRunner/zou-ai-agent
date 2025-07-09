package wiki.zhr.zouaiagent.reader;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 这这里输入你的飞书配置
 */
@Component
@Data
public class FeiShuConfig {

    public  String FEISHU_APP_ID = System.getenv("FEISHU_APP_ID");

    public String FEISHU_APP_SECRET = System.getenv("FEISHU_APP_SECRET");

    public String FEISHU_DOCUMENT_ID = System.getenv("FEISHU_DOCUMENT_ID");

    public String FEISHU_USER_TOKEN; // 通过OAuth动态设置

}
