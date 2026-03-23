package jar.controller;

import com.opencsv.CSVReader;
import jar.dto.PracticeTopicRequest;
import jar.entity.PracticeCategory;
import jar.entity.PracticeQuestion;
import jar.entity.PracticeTopic;
import jar.repository.PracticeCategoryRepository;
import jar.repository.PracticeQuestionRepository;
import jar.repository.PracticeTopicRepository;
import org.springframework.http.ResponseEntity;
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

    public AdminPracticeController(PracticeCategoryRepository categoryRepo,
                                   PracticeTopicRepository topicRepo,
                                   PracticeQuestionRepository questionRepo) {
        this.categoryRepo = categoryRepo;
        this.topicRepo = topicRepo;
        this.questionRepo = questionRepo;
    }

    @GetMapping("/categories")
    public List<PracticeCategory> getCategories() {
        return categoryRepo.findAll();
    }

    @PostMapping("/categories")
    public PracticeCategory createCategory(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "").trim();
        if (name.isBlank()) {
            throw new RuntimeException("Category name is required");
        }
        return categoryRepo.save(PracticeCategory.builder().name(name).build());
    }

    @PostMapping("/topics")
    public PracticeTopic createTopic(@RequestBody PracticeTopicRequest request) {
        PracticeCategory category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        PracticeTopic topic = PracticeTopic.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .build();
        return topicRepo.save(topic);
    }

    @PostMapping("/questions/bulk")
    public ResponseEntity<?> bulkUploadQuestions(@RequestParam("file") MultipartFile file) {
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
                        .questionText(row[1].trim())
                        .options(row[2].trim())
                        .correctAnswer(row[3].trim())
                        .explanation(row[4].trim())
                        .difficulty(row[5].trim())
                        .type(row[6].trim())
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
        return ResponseEntity.ok(response);
    }
}
