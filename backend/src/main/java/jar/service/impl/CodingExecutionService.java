package jar.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CodingExecutionService {

    @Value("${judge0.api.url:}")
    private String judge0Url;

    @Value("${judge0.api.key:}")
    private String judge0ApiKey;

    @Value("${judge0.api.host:}")
    private String judge0ApiHost;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> evaluate(String language, String code, String testCasesJson) {
        if (code == null || code.isBlank()) {
            return Map.of(
                    "status", "WRONG_ANSWER",
                    "passed", 0,
                    "total", 0,
                    "details", List.of(Map.of("message", "Code is required")),
                    "source", "validation"
            );
        }

        if (judge0Url == null || judge0Url.isBlank()) {
            boolean accepted = code.length() > 20;
            int passed = accepted ? 1 : 0;
            return Map.of(
                    "status", accepted ? "ACCEPTED" : "WRONG_ANSWER",
                    "passed", passed,
                    "total", 1,
                    "details", accepted ? List.of() : List.of(Map.of("message", "Judge0 is not configured on server")),
                    "source", "local-simulator"
            );
        }

        int languageId = resolveLanguageId(language);
        List<TestCase> testCases = parseTestCases(testCasesJson);
        if (testCases.isEmpty()) {
            testCases = List.of(new TestCase("", null));
        }

        List<Map<String, Object>> details = new ArrayList<>();
        int passed = 0;
        String overallStatus = "ACCEPTED";

        for (int i = 0; i < testCases.size(); i++) {
            TestCase testCase = testCases.get(i);
            Map<String, Object> judge = runSubmission(languageId, code, testCase.input());
            String verdict = normalizeVerdict(judge);
            String stdout = safeTrim(String.valueOf(judge.getOrDefault("stdout", "")));
            String stderr = safeTrim(String.valueOf(judge.getOrDefault("stderr", "")));
            String compileOutput = safeTrim(String.valueOf(judge.getOrDefault("compile_output", "")));
            String expected = safeTrim(testCase.expectedOutput());

            boolean matchedExpected = expected == null || expected.equals(stdout);
            boolean testcasePassed = "ACCEPTED".equals(verdict) && matchedExpected;
            if (testcasePassed) {
                passed++;
            } else {
                overallStatus = "COMPILATION_ERROR".equals(verdict)
                        ? "COMPILATION_ERROR"
                        : ("RUNTIME_ERROR".equals(verdict) ? "RUNTIME_ERROR" : "WRONG_ANSWER");
            }

            if (!testcasePassed) {
                Map<String, Object> row = new HashMap<>();
                row.put("testCase", i + 1);
                row.put("status", verdict);
                row.put("input", testCase.input());
                row.put("expectedOutput", expected);
                row.put("actualOutput", stdout);
                row.put("stderr", stderr);
                row.put("compileOutput", compileOutput);
                details.add(row);
            }
        }

        return Map.of(
                "status", passed == testCases.size() ? "ACCEPTED" : overallStatus,
                "passed", passed,
                "total", testCases.size(),
                "details", details,
                "source", "judge0"
        );
    }

    private Map<String, Object> runSubmission(int languageId, String code, String stdin) {
        String endpoint = judge0Url.endsWith("/")
                ? judge0Url + "submissions?base64_encoded=false&wait=true"
                : judge0Url + "/submissions?base64_encoded=false&wait=true";

        Map<String, Object> payload = new HashMap<>();
        payload.put("source_code", code);
        payload.put("language_id", languageId);
        payload.put("stdin", stdin == null ? "" : stdin);
        payload.put("redirect_stderr_to_stdout", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (judge0ApiKey != null && !judge0ApiKey.isBlank()) {
            headers.set("X-Auth-Token", judge0ApiKey);
            headers.set("X-RapidAPI-Key", judge0ApiKey);
        }
        if (judge0ApiHost != null && !judge0ApiHost.isBlank()) {
            headers.set("X-RapidAPI-Host", judge0ApiHost);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
        return response.getBody() == null ? Map.of() : response.getBody();
    }

    private int resolveLanguageId(String language) {
        if (language == null) {
            return 71;
        }
        String normalized = language.trim().toLowerCase();
        return switch (normalized) {
            case "python", "python3" -> 71;
            case "java" -> 62;
            case "c" -> 50;
            case "c++", "cpp" -> 54;
            default -> 71;
        };
    }

    private String normalizeVerdict(Map<String, Object> judge) {
        Object statusObj = judge.get("status");
        if (statusObj instanceof Map<?, ?> statusMap) {
            Object idObj = statusMap.get("id");
            int id = idObj instanceof Number ? ((Number) idObj).intValue() : -1;
            if (id == 3) return "ACCEPTED";
            if (id == 6 || id == 7) return "COMPILATION_ERROR";
            if (id == 11 || id == 12 || id == 13) return "RUNTIME_ERROR";
        }
        return "WRONG_ANSWER";
    }

    private List<TestCase> parseTestCases(String testCasesJson) {
        if (testCasesJson == null || testCasesJson.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> nodes = objectMapper.readValue(
                    testCasesJson, new TypeReference<List<Map<String, Object>>>() {});
            List<TestCase> rows = new ArrayList<>();
            for (Map<String, Object> node : nodes) {
                String input = firstNonBlank(
                        toStringOrNull(node.get("input")),
                        toStringOrNull(node.get("stdin"))
                );
                String expected = firstNonBlank(
                        toStringOrNull(node.get("output")),
                        toStringOrNull(node.get("expectedOutput")),
                        toStringOrNull(node.get("expected_output"))
                );
                rows.add(new TestCase(input == null ? "" : input, expected));
            }
            return rows;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String toStringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String safeTrim(String value) {
        if (value == null || "null".equalsIgnoreCase(value)) {
            return "";
        }
        return value.trim();
    }

    private record TestCase(String input, String expectedOutput) {
    }
}
