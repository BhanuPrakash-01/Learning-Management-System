package jar.controller;

import com.opencsv.CSVReader;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/practice")
public class AdminPracticeController {

    private final PracticeCategoryRepository categoryRepo;
    private final PracticeTopicRepository topicRepo;
    private final PracticeQuestionRepository questionRepo;
    private final InputSanitizerService sanitizer;
    private final AdminAuditService adminAuditService;

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
    public List<PracticeCategory> getCategories() {
        return categoryRepo.findAll();
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
        PracticeCategory category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        PracticeTopic topic = PracticeTopic.builder()
                .category(category)
                .name(sanitizer.sanitizePlainText(request.getName()))
                .description(sanitizer.sanitizeRichText(request.getDescription()))
                .icon(sanitizer.sanitizePlainText(request.getIcon()))
                .build();
        PracticeTopic saved = topicRepo.save(topic);
        adminAuditService.log(auth.getName(), "CREATE_PRACTICE_TOPIC", "PRACTICE_TOPIC",
                String.valueOf(saved.getId()), "Created topic " + saved.getName());
        return saved;
    }

    @GetMapping("/questions/template")
    public ResponseEntity<String> practiceTemplate() {
        String csv = "topicId,questionText,optionsJson,correctAnswer,explanation,difficulty,type\n"
                + "1,\"What is 2+2?\",\"[\\\"1\\\",\\\"2\\\",\\\"3\\\",\\\"4\\\"]\",4,\"Basic arithmetic\",EASY,MCQ\n";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=practice_questions_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping("/questions/bulk")
    public ResponseEntity<?> bulkUploadQuestions(@RequestParam("file") MultipartFile file,
                                                 Authentication auth) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty"));
        }
        int created = 0;
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] row;
            int rowNo = 0;
            while ((row = reader.readNext()) != null) {
                rowNo++;
                if (rowNo == 1 && row[0].toLowerCase().contains("topicid")) {
                    continue;
                }
                if (row.length < 7) {
                    throw new RuntimeException("Row " + rowNo + " should have 7 columns");
                }
                Long topicId = Long.parseLong(row[0].trim());
                PracticeTopic topic = topicRepo.findById(topicId)
                        .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));

                PracticeQuestion question = PracticeQuestion.builder()
                        .topic(topic)
                        .questionText(sanitizer.sanitizeRichText(row[1].trim()))
                        .options(sanitizer.sanitizePlainText(row[2].trim()))
                        .correctAnswer(sanitizer.sanitizePlainText(row[3].trim()))
                        .explanation(sanitizer.sanitizeRichText(row[4].trim()))
                        .difficulty(sanitizer.sanitizePlainText(row[5].trim()))
                        .type(sanitizer.sanitizePlainText(row[6].trim()))
                        .build();
                questionRepo.save(question);
                created++;
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Practice questions uploaded");
        response.put("count", created);
        adminAuditService.log(auth.getName(), "BULK_UPLOAD_PRACTICE_QUESTION", "PRACTICE_QUESTION",
                String.valueOf(created), "Uploaded practice questions");
        return ResponseEntity.ok(response);
    }
}
