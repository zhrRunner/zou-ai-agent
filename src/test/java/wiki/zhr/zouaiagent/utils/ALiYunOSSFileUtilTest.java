package wiki.zhr.zouaiagent.utils;

import com.aliyuncs.exceptions.ClientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ALiYunOSSFileUtilTest {

    @Test
    void uploadFile() {
        ALiYunOSSFileUtil aLiYunOSSFileUtil = ALiYunOSSFileUtil.builder()
                .bucketName("zou-ai-agent")
                .objectName("userName/jujingyi1.png")
                .filePath(System.getProperty("user.dir")+"/tmp/download/jujingyi.png")
                .endpoint("https://oss-cn-beijing.aliyuncs.com")
                .region("cn-beijing")
                .build();

        try {
            String result = aLiYunOSSFileUtil.uploadFile();
            assertNotNull(result);
        } catch (ClientException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}