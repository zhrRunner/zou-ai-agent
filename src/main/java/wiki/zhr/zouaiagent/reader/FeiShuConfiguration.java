package wiki.zhr.zouaiagent.reader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeiShuConfiguration {

    @Value("${feishu.app-id}")
    private String appId;

    @Value("${feishu.app-secret}")
    private String appSecret;

    @Bean
    public FeiShuResource feiShuResource() {
        return FeiShuResource.builder()
                .appId(appId)
                .appSecret(appSecret)
                .build();
    }
}