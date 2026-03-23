package jar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    @GetMapping
    public Map<String, Object> settings() {
        return Map.of(
                "emailDomain", "@anurag.ac.in",
                "branches", List.of("CSE", "CSE-AI", "ECE", "EEE", "MECH", "CIVIL", "IT", "DS"),
                "batchYears", List.of(2022, 2023, 2024, 2025, 2026, 2027),
                "sections", List.of("A", "B", "C", "D", "E")
        );
    }
}
