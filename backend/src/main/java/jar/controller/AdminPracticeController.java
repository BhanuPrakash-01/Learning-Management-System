package jar.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.transaction.Transactional;
import jar.dto.PracticeTopicRequest;
import jar.entity.PracticeCategory;
import jar.entity.PracticeQuestion;
import jar.entity.PracticeTopic;
import jar.repository.PracticeCategoryRepository;
import jar.repository.PracticeQuestionRepository;
import jar.repository.PracticeTopicRepository;
import jar.service.security.AdminAuditService;
import jar.service.security.InputSanitizerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin/practice")
public class AdminPracticeController {

    private final PracticeCategoryRepository categoryRepo;
    private final PracticeTopicRepository topicRepo;
    private final PracticeQuestionRepository questionRepo;
    private final InputSanitizerService sanitizer;
    private final AdminAuditService adminAuditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminPracticeController(PracticeCategoryRepository categoryRepo,
                                   PracticeTopicRepository topicRepo,
                                   PracticeQuestionRepository questionRepo,
                                   InputSanitizerService sanitizer,
                                   AdminAuditService adminAuditService) {
        this.categoryRepo = categoryRepo;
        this.topicRepo = topicRepo;
        this.questionRepo = questionRepo;
        this.sanitizer = sanitizer;
        this.adminAuditService = adminAuditService;
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> getCategories() {
        return categoryRepo.findAll().stream().map(category -> {
            List<Map<String, Object>> topics = topicRepo.findByCategory(category).stream()
                    .map(this::toTopicSummary)
                    .toList();

            Map<String, Object> row = new HashMap<>();
            row.put("id", category.getId());
            row.put("name", category.getName());
            row.put("topics", topics);
            return row;
        }).toList();
    }

    @PostMapping("/categories")
    public PracticeCategory createCategory(@RequestBody Map<String, String> body, Authentication auth) {
        String name = sanitizer.sanitizePlainText(body.getOrDefault("name", ""));
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Category name is required");
        }
        PracticeCategory saved = categoryRepo.save(PracticeCategory.builder().name(name).build());
        adminAuditService.log(auth.getName(), "CREATE_PRACTICE_CATEGORY", "PRACTICE_CATEGORY",
                String.valueOf(saved.getId()), "Created category " + saved.getName());
        return saved;
    }

    @PostMapping("/topics")
    public PracticeTopic createTopic(@RequestBody PracticeTopicRequest request, Authentication auth) {
        PracticeTopic saved = saveTopic(request);
        adminAuditService.log(auth.getName(), "CREATE_PRACTICE_TOPIC", "PRACTICE_TOPIC",
                String.valueOf(saved.getId()), "Created topic " + saved.getName());
        return saved;
    }

    @PostMapping(value = "/topics/with-questions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> createTopicWithQuestions(@RequestPart("topicJson") PracticeTopicRequest request,
                                                      @RequestPart(value = "file", required = false) MultipartFile file,
                                                      Authentication auth) {
        try {
            PracticeTopic topic = saveTopic(request);
            List<PracticeQuestion> questions = List.of();
            if (file != null && !file.isEmpty()) {
                questions = parsePracticeQuestions(file, topic);
                questionRepo.saveAll(questions);
            }

            adminAuditService.log(auth.getName(), "CREATE_TOPIC_WITH_QUESTIONS", "PRACTICE_TOPIC",
                    String.valueOf(topic.getId()),
                    "Created topic " + topic.getName() + " with " + questions.size() + " questions");

            return ResponseEntity.ok(Map.of(
                    "topic", toTopicSummary(topic),
                    "questionCount", questions.size(),
                    "message", topic.getName() + " created with " + questions.size() + " questions"
            ));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @PatchMapping("/topics/{id}/toggle-active")
    public PracticeTopic toggleTopicActive(@PathVariable Long id, Authentication auth) {
        PracticeTopic topic = topicRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found"));
        topic.setActive(!Boolean.TRUE.equals(topic.getActive()));
        PracticeTopic saved = topicRepo.save(topic);

        adminAuditService.log(auth.getName(), "TOGGLE_PRACTICE_TOPIC_ACTIVE", "PRACTICE_TOPIC",
                String.valueOf(saved.getId()), "Set topic active=" + Boolean.TRUE.equals(saved.getActive()));
        return saved;
    }

    @GetMapping("/questions/template")
    public ResponseEntity<String> practiceTemplate() {
        String csv = "questionText,optionsJson,correctAnswer,explanation,difficulty,type\n"
                + "\"What is 2+2?\",\"[\\\"1\\\",\\\"2\\\",\\\"3\\\",\\\"4\\\"]\",4,\"Basic arithmetic\",EASY,MCQ\n";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=practice_questions_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping("/questions/bulk")
    public ResponseEntity<?> bulkUploadQuestions(@RequestParam("topicId") Long topicId,
                                                 @RequestParam("file") MultipartFile file,
                                                 Authentication auth) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty"));
        }

        try {
            PracticeTopic topic = topicRepo.findById(topicId)
                    .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));

            List<PracticeQuestion> questions = parsePracticeQuestions(file, topic);
            questionRepo.saveAll(questions);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Practice questions uploaded");
            response.put("count", questions.size());

            adminAuditService.log(auth.getName(), "BULK_UPLOAD_PRACTICE_QUESTION", "PRACTICE_QUESTION",
                    String.valueOf(questions.size()),
                    "Uploaded " + questions.size() + " questions to topic " + topic.getName());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private PracticeTopic saveTopic(PracticeTopicRequest request) {
        PracticeCategory category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        String topicName = sanitizer.sanitizePlainText(request.getName());
        if (topicName == null || topicName.isBlank()) {
            throw new RuntimeException("Topic name is required");
        }

        PracticeTopic topic = PracticeTopic.builder()
                .category(category)
                .name(topicName)
                .description(sanitizer.sanitizeRichText(request.getDescription()))
                .icon(sanitizer.sanitizePlainText(request.getIcon()))
                .active(true)
                .build();

        return topicRepo.save(topic);
    }

    private List<PracticeQuestion> parsePracticeQuestions(MultipartFile file,
                                                          PracticeTopic topic) throws Exception {
        List<PracticeQuestion> parsed = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] row;
            int rowNo = 0;
            while ((row = reader.readNext()) != null) {
                rowNo++;

                if (row.length == 0 || (row.length == 1 && row[0].isBlank())) {
                    continue;
                }

                String firstCell = stripBom(row[0]).trim();
                if (rowNo == 1 && firstCell.toLowerCase().contains("question")) {
                    continue;
                }

                if (row.length < 3) {
                    errors.add("Row " + rowNo + ": expected at least 3 columns (questionText, optionsJson, correctAnswer)");
                    continue;
                }

                String questionText = sanitizer.sanitizePlainTextPermissive(firstCell);
                if (questionText == null || questionText.isBlank()) {
                    errors.add("Row " + rowNo + ": questionText is empty after sanitization");
                    continue;
                }

                String optionsJson = row[1].trim();
                List<String> options = parseOptionsArray(optionsJson, rowNo, errors);
                if (options == null) {
                    continue;
                }

                String correctAnswer = sanitizer.sanitizePlainTextPermissive(row[2].trim());
                if (correctAnswer == null || correctAnswer.isBlank()) {
                    errors.add("Row " + rowNo + ": correctAnswer is required");
                    continue;
                }

                boolean answerPresent = options.stream().filter(Objects::nonNull)
                        .anyMatch(option -> option.equalsIgnoreCase(correctAnswer));
                if (!answerPresent) {
                    errors.add("Row " + rowNo + ": correctAnswer must match one value in optionsJson");
                    continue;
                }

                String explanation = row.length > 3 ? sanitizer.sanitizeRichText(row[3].trim()) : null;
                String difficulty = row.length > 4 ? sanitizer.sanitizePlainText(row[4].trim()) : "Medium";
                String type = row.length > 5 ? sanitizer.sanitizePlainText(row[5].trim()) : "MCQ";

                parsed.add(PracticeQuestion.builder()
                        .topic(topic)
                        .questionText(questionText)
                        .options(objectMapper.writeValueAsString(options))
                        .correctAnswer(correctAnswer)
                        .explanation(explanation)
                        .difficulty(difficulty == null ? "Medium" : difficulty)
                        .type(type == null ? "MCQ" : type)
                        .build());
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException("CSV parse error: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("Upload failed. Fix these issues and try again:\n" + String.join("\n", errors));
        }
        if (parsed.isEmpty()) {
            throw new RuntimeException("Upload failed. No valid question rows were found in the CSV.");
        }

        return parsed;
    }

    private List<String> parseOptionsArray(String rawOptions,
                                           int rowNo,
                                           List<String> errors) {
        try {
            JsonNode jsonNode = objectMapper.readTree(rawOptions);
            if (!jsonNode.isArray()) {
                errors.add("Row " + rowNo + ": optionsJson must be a JSON array");
                return null;
            }

            List<String> options = new ArrayList<>();
            for (JsonNode optionNode : jsonNode) {
                String value = sanitizer.sanitizePlainTextPermissive(optionNode.asText());
                if (value == null || value.isBlank()) {
                    errors.add("Row " + rowNo + ": optionsJson contains an empty option");
                    return null;
                }
                options.add(value);
            }

            if (options.isEmpty()) {
                errors.add("Row " + rowNo + ": optionsJson must contain at least one option");
                return null;
            }

            return options;
        } catch (Exception e) {
            errors.add("Row " + rowNo + ": optionsJson is invalid JSON");
            return null;
        }
    }

    private Map<String, Object> toTopicSummary(PracticeTopic topic) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", topic.getId());
        row.put("categoryId", topic.getCategory() == null ? null : topic.getCategory().getId());
        row.put("name", topic.getName());
        row.put("description", topic.getDescription());
        row.put("icon", topic.getIcon());
        row.put("active", Boolean.TRUE.equals(topic.getActive()));
        row.put("questionCount", questionRepo.countByTopic(topic));
        return row;
    }

    private String stripBom(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "");
    }
}
