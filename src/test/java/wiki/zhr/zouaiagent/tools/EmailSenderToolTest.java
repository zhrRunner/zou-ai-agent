package wiki.zhr.zouaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmailSenderToolTest {

    @Test
    void sendEmailWithAttachment() {
        EmailSenderTool emailSenderTool = new EmailSenderTool();
        String to = "18713658382@163.com";
        String subject = "恭喜你成为世界上最帅的男人";
        String content = "心有灵犀一点通，想往幸福意朦胧，事事顺心喜相逢，成功就在努力中" +
                "   平步青云咯！\n";
        String attachmentfile = System.getProperty("user.dir") + "/tmp/pdf/点我哟.pdf";
        String result = emailSenderTool.sendEmailWithAttachment(to, subject, content, attachmentfile);
        System.out.println(result);
        assertNotNull(result);
    }
}