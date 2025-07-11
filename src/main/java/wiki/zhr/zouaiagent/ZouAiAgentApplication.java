package wiki.zhr.zouaiagent;

import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
public class ZouAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZouAiAgentApplication.class, args);
    }

}
