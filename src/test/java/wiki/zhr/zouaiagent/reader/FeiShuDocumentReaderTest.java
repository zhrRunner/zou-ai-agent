
package wiki.zhr.zouaiagent.reader;

import jakarta.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Test class for FeiShuDocumentReader. Tests will be skipped if FEISHU_APP_ID and
 * FEISHU_APP_SECRET environment variables are not set.
 */
public class FeiShuDocumentReaderTest {

	private static final Logger log = LoggerFactory.getLogger(FeiShuDocumentReaderTest.class);


	// Get configuration from environment variables
	private static final String FEISHU_APP_ID = System.getenv("FEISHU_APP_ID");

	private static final String FEISHU_APP_SECRET = System.getenv("FEISHU_APP_SECRET");

	// Optional user token and document ID from environment variables
	private  String FEISHU_USER_TOKEN;

	private static final String FEISHU_DOCUMENT_ID = System.getenv("FEISHU_DOCUMENT_ID");

	private FeiShuDocumentReader feiShuDocumentReader;

	private FeiShuResource feiShuResource;

	static {
		if (FEISHU_APP_ID == null || FEISHU_APP_SECRET == null) {
			System.out
				.println("FEISHU_APP_ID or FEISHU_APP_SECRET environment variable is not set. Tests will be skipped.");
		}
	}

	@BeforeEach
	void setup() {
		// Skip test if environment variables are not set
		Assumptions.assumeTrue(FEISHU_APP_ID != null && !FEISHU_APP_ID.isEmpty(),
				"Skipping test because FEISHU_APP_ID is not set");
		Assumptions.assumeTrue(FEISHU_APP_SECRET != null && !FEISHU_APP_SECRET.isEmpty(),
				"Skipping test because FEISHU_APP_SECRET is not set");

		// Create FeiShuResource with environment variables
		feiShuResource = FeiShuResource.builder().appId(FEISHU_APP_ID).appSecret(FEISHU_APP_SECRET).build();

        try {
            FEISHU_USER_TOKEN = getFeiShuUserToken();
        } catch (IOException e) {
			System.out.println("Failed to read FeiShu user token from file: " + e.getMessage());
        }
    }

	@Test
	void feiShuDocumentTest() {
		System.out.println(String.format("FEISHU_APP_ID = %s, FEISHU_APP_SECRET = %s, FEISHU_DOCUMENT_ID = %s",
				FEISHU_APP_ID, FEISHU_APP_SECRET, FEISHU_DOCUMENT_ID));
		System.out.println("feiShuResource = " + feiShuResource);
		feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource);
		List<Document> documentList = feiShuDocumentReader.get();
		log.info("feiShuDocumentTest result:{}", documentList);
	}

	@Test
	void feiShuDocumentTestByUserToken() {
		// Skip test if user token is not set
		Assumptions.assumeTrue(FEISHU_USER_TOKEN != null && !FEISHU_USER_TOKEN.isEmpty(),
				"Skipping test because FEISHU_USER_TOKEN is not set");

		feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource, FEISHU_USER_TOKEN);
		List<Document> documentList = feiShuDocumentReader.get();
		log.info("feiShuDocumentTestByUserToken result:{}", documentList);
	}

	@Test
	void feiShuDocumentTestByUserTokenAndDocumentId() {
		// Skip test if user token or document ID is not set
		Assumptions.assumeTrue(FEISHU_USER_TOKEN != null && !FEISHU_USER_TOKEN.isEmpty(),
				"Skipping test because FEISHU_USER_TOKEN is not set");
		Assumptions.assumeTrue(FEISHU_DOCUMENT_ID != null && !FEISHU_DOCUMENT_ID.isEmpty(),
				"Skipping test because FEISHU_DOCUMENT_ID is not set");

		feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource, FEISHU_USER_TOKEN, FEISHU_DOCUMENT_ID);
		List<Document> documentList = feiShuDocumentReader.get();
		log.info("feiShuDocumentTestByUserTokenAndDocumentId result:{}", documentList);
	}

	private String getFeiShuUserToken() throws IOException {
		// 从tmp/feishu_user_token.txt文件中读取FEISHU_USER_TOKEN
		String token = FileUtils.readFileToString(new File(System.getProperty("user.dir") + "/tmp/feishu_user_token.txt"), "UTF-8");
		return token;
	}

}