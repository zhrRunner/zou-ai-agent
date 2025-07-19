package wiki.zhr.zouaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ZouManusTest {

    @Resource
    private ZouManus zouManus;

    @Test
    void run() {
        String userPrompt = """  
                1. 请分析以下代码，生成单元测试和代码审查报告，
                单元测试代码写入本地文件，格式.md，审查报告写入本地文件.md，用中文!!!
                2. 然后，请你在网上搜索和mac os相关的图片
                3. 请列出 https://github.com/zhrRunner 下的所有仓库，生成本地文件.md
                4. zou-ai-agent 是我写的一个开源项目，请你帮我分析一下这个项目的代码，给出这个项目的架构设计，生成md文档
         
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
        String answer = zouManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}
