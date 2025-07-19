package wiki.zhr.zouaiagent.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wiki.zhr.zouaiagent.tools.CodeCoverageAnalyzer;

import java.util.Map;

@RestController
@RequestMapping("/coverage")
public class CodeCoverageController {

    @Autowired
    private CodeCoverageAnalyzer analyzer;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeCoverage(@RequestBody Map<String, String> request) {
        try {
            String sourceCode = request.get("sourceCode");
            String testCode = request.get("testCode");

            if (sourceCode == null || testCode == null) {
                return ResponseEntity.badRequest().body("sourceCode and testCode are required");
            }

            CodeCoverageAnalyzer.CoverageResult result = analyzer.analyzeCoverage(sourceCode, testCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error analyzing coverage: " + e.getMessage());
        }
    }
}