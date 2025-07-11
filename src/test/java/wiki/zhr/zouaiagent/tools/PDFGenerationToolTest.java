package wiki.zhr.zouaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool pdfTool = new PDFGenerationTool();
        String fileName = "点我哟.pdf";
        String content = "zouhr是你爹！！！";
        String result = pdfTool.generatePDF(fileName, content);
        assertNotNull(result);
        assertTrue(result.contains("PDF generated successfully"));
    }
}