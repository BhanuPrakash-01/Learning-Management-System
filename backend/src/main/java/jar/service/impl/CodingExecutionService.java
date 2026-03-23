package jar.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CodingExecutionService {

    @Value("${judge0.api.url:}")
    private String judge0Url;

    public Map<String, Object> evaluate(String language, String code, String testCasesJson) {
        // Cloud integration placeholder:
        // if judge0.api.url is configured, this service can be extended to call Judge0.
        // For now, we provide deterministic sandbox-free evaluation for app flow completeness.
        boolean accepted = code != null && code.length() > 20;
        int passed = accepted ? 5 : 0;
        return Map.of(
                "status", accepted ? "ACCEPTED" : "WRONG_ANSWER",
                "passed", passed,
                "source", judge0Url == null || judge0Url.isBlank() ? "local-simulator" : "judge0-adapter-ready"
        );
    }
}
