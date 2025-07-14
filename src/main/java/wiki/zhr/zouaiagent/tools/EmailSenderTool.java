package wiki.zhr.zouaiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.mail.MailUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class EmailSenderTool {

    @Tool(description = "发送带附件的电子邮件")
    public String sendEmailWithAttachment(
            @ToolParam(description = "收件人邮箱地址") String to,
            @ToolParam(description = "邮件主题") String subject,
            @ToolParam(description = "邮件正文内容") String content,
            @ToolParam(description = "附件文件路径") String filePath) {

        try {
            // 检查附件是否存在
            if (!FileUtil.exist(filePath)) {
                return "附件文件不存在: " + filePath;
            }

            MailUtil.send(to, subject, content, false, new File(filePath));
            return "带附件邮件发送成功至: " + to;
        } catch (Exception e) {
            return "带附件邮件发送失败: " + e.getMessage();
        }
    }
}