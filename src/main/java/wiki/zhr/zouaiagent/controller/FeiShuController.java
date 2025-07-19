package wiki.zhr.zouaiagent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wiki.zhr.zouaiagent.reader.FeiShuDocumentReader;
import wiki.zhr.zouaiagent.reader.FeiShuResource;
import org.springframework.ai.document.Document;

import java.util.Map;

@RestController
@RequestMapping("/feishu")
public class FeiShuController {

    private final FeiShuResource feiShuResource;

    public FeiShuController(FeiShuResource feiShuResource) {
        this.feiShuResource = feiShuResource;
    }

    @PostMapping("/document")
    public ResponseEntity<?> getDocument(@RequestBody Map<String, String> request) {
        try {
            String userAccessToken = request.get("userAccessToken");
            String documentId = request.get("documentId");

            if (userAccessToken == null || documentId == null) {
                return ResponseEntity.badRequest().body("userAccessToken and documentId are required");
            }

            FeiShuDocumentReader reader = new FeiShuDocumentReader(feiShuResource, userAccessToken, documentId);
            Document document = reader.getDocumentContentByUser(documentId, userAccessToken);

            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error getting document: " + e.getMessage());
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<?> getDocumentList(@RequestParam String userAccessToken) {
        try {
            if (userAccessToken == null) {
                return ResponseEntity.badRequest().body("userAccessToken is required");
            }

            FeiShuDocumentReader reader = new FeiShuDocumentReader(feiShuResource, userAccessToken);
            Document documentList = reader.getDocumentListByUser(userAccessToken);

            return ResponseEntity.ok(documentList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error getting document list: " + e.getMessage());
        }
    }
}