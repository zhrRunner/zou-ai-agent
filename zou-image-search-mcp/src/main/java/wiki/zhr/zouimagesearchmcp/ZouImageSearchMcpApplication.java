package wiki.zhr.zouimagesearchmcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import wiki.zhr.zouimagesearchmcp.tools.ImageSearchTool;

@SpringBootApplication
public class ZouImageSearchMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZouImageSearchMcpApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(imageSearchTool)
				.build();
	}
}


