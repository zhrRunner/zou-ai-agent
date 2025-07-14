package wiki.zhr.zouaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmailSenderToolTest {

    @Test
    void sendEmailWithAttachment() {
        EmailSenderTool emailSenderTool = new EmailSenderTool();
        String to = "438345469@qq.com";
        String subject = "恭喜你成为世界上最美的女人";
        String content = "心有灵犀一点通，想往幸福意朦胧，事事顺心喜相逢，成功就在努力中";
        String attachmentfile = System.getProperty("user.dir") + "/tmp/pdf/点我哟.pdf";
        String result = emailSenderTool.sendEmailWithAttachment(to, subject, content, attachmentfile);
        System.out.println(result);
        assertNotNull(result);
    }
}