package jar.service.impl;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import jar.dto.QuestionRequest;
import jar.entity.Assessment;
import jar.entity.Question;
import jar.repository.AssessmentRepository;
import jar.repository.QuestionRepository;
import jar.service.QuestionService;
import jar.service.security.InputSanitizerService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepo;
    private final AssessmentRepository assessmentRepo;
    private final InputSanitizerService sanitizer;

    // ✅ Valid values for the correctAnswer column
    private static final Set<String> VALID_ANSWERS = Set.of("A", "B", "C", "D");

    public QuestionServiceImpl(QuestionRepository questionRepo,
                               AssessmentRepository assessmentRepo,
                               InputSanitizerService sanitizer) {
        this.questionRepo = questionRepo;
        this.assessmentRepo = assessmentRepo;
        this.sanitizer = sanitizer;
    }

    @Override
    public Question addQuestion(QuestionRequest request) {
        Assessment assessment = assessmentRepo.findById(request.getAssessmentId())
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        String questionText = sanitizer.sanitizePlainTextPermissive(request.getQuestionText());
        if (questionText == null) {
            throw new RuntimeException("Question text is required");
        }

        Question question = Question.builder()
                .questionText(questionText)
                .optionA(sanitizer.sanitizePlainText(request.getOptionA()))
                .optionB(sanitizer.sanitizePlainText(request.getOptionB()))
                .optionC(sanitizer.sanitizePlainText(request.getOptionC()))
                .optionD(sanitizer.sanitizePlainText(request.getOptionD()))
                .correctAnswer(sanitizer.sanitizePlainText(request.getCorrectAnswer()))
                .subject(sanitizer.sanitizePlainText(request.getSubject()))
                .topic(sanitizer.sanitizePlainText(request.getTopic()))
                .difficulty(sanitizer.sanitizePlainText(request.getDifficulty()))
                .assessment(assessment)
                .build();

        return questionRepo.save(question);
    }

    @Override
    public List<Question> getAll() {
        return questionRepo.findAll();
    }

    @Override
    public Page<Question> getAll(Pageable pageable) {
        return questionRepo.findAll(pageable);
    }

    @Override
    public List<Question> getByAssessment(Long assessmentId) {
        Assessment assessment = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
        return questionRepo.findByAssessment(assessment);
    }

    @Override
    public Page<Question> searchLibrary(String search,
                                        Long assessmentId,
                                        String difficulty,
                                        String topic,
                                        Pageable pageable) {
        return questionRepo.searchLibrary(search, assessmentId, difficulty, topic, pageable);
    }

    @Override
    public Map<Long, Long> assessmentQuestionCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : questionRepo.countByAssessmentGrouped()) {
            if (row[0] == null || row[1] == null) {
                continue;
            }
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public Question updateQuestion(Long id, QuestionRequest request) {
        Question question = questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Assessment assessment = assessmentRepo.findById(request.getAssessmentId())
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        String questionText = sanitizer.sanitizePlainTextPermissive(request.getQuestionText());
        if (questionText == null) {
            throw new RuntimeException("Question text is required");
        }

        question.setQuestionText(questionText);
        question.setOptionA(sanitizer.sanitizePlainText(request.getOptionA()));
        question.setOptionB(sanitizer.sanitizePlainText(request.getOptionB()));
        question.setOptionC(sanitizer.sanitizePlainText(request.getOptionC()));
        question.setOptionD(sanitizer.sanitizePlainText(request.getOptionD()));
        question.setCorrectAnswer(sanitizer.sanitizePlainText(request.getCorrectAnswer()));
        question.setSubject(sanitizer.sanitizePlainText(request.getSubject()));
        question.setTopic(sanitizer.sanitizePlainText(request.getTopic()));
        question.setDifficulty(sanitizer.sanitizePlainText(request.getDifficulty()));
        question.setAssessment(assessment);

        return questionRepo.save(question);
    }

    @Override
    public void deleteQuestion(Long id) {
        questionRepo.deleteById(id);
    }

    @Override
    public void deleteQuestions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        questionRepo.deleteAllById(ids);
    }

    // ✅ NEW: Bulk CSV upload
    //
    // HOW REAL-WORLD LMS PLATFORMS HANDLE THIS:
    //   - Canvas uses QTI XML format (complex but powerful)
    //   - Moodle supports Aiken, GIFT, and XML formats
    //   - Google Forms uses CSV/Sheets directly
    //   - Most mid-size platforms use CSV because it's universal —
    //     anyone can prepare it in Excel or Google Sheets
    //
    // EXPECTED CSV FORMAT (first row is a header, skipped automatically):
    //   questionText, optionA, optionB, optionC, optionD, correctAnswer, assessmentId
    //   "What is Java?","Language","Coffee","Island","Car","A",1
    //   "2+2=?","3","4","5","6","B",1
    //
    // DESIGN DECISIONS:
    //   1. We skip the header row by checking if the first row starts with
    //      "questionText" (case-insensitive) — this avoids off-by-one errors
    //      if admins include or exclude the header.
    //   2. We validate each row before saving and collect errors, then throw
    //      a single exception listing ALL bad rows so the admin can fix
    //      everything in one pass (not discover errors one at a time).
    //   3. We use OpenCSV which handles quoted commas correctly — e.g.
    //      "What is the answer to, this question?" won't break the parse.
    //   4. All saves happen after all validations pass — so we never get
    //      partial uploads that leave the DB in an inconsistent state.
    @Override
    public List<Question> bulkUpload(MultipartFile file) {
        return bulkUploadInternal(file, null);
    }

    @Override
    public List<Question> bulkUpload(MultipartFile file, Assessment assessment) {
        if (assessment == null || assessment.getId() == null) {
            throw new RuntimeException("Assessment is required for this upload flow");
        }
        return bulkUploadInternal(file, assessment);
    }

    private List<Question> bulkUploadInternal(MultipartFile file, Assessment fixedAssessment) {
        validateCsvFile(file);

        List<String> errors = new ArrayList<>();
        List<Question> toSave = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            int rowNumber = 0;

            while ((line = reader.readNext()) != null) {
                rowNumber++;

                if (line.length == 0 || (line.length == 1 && line[0].isBlank())) {
                    continue;
                }

                String firstCell = stripBom(line[0]).trim();
                if (rowNumber == 1 && firstCell.toLowerCase(Locale.ROOT).startsWith("question")) {
                    continue;
                }

                if (fixedAssessment == null && line.length < 7) {
                    errors.add("Row " + rowNumber + ": expected at least 7 columns, got " + line.length);
                    continue;
                }
                if (fixedAssessment != null && line.length < 6) {
                    errors.add("Row " + rowNumber + ": expected at least 6 columns, got " + line.length);
                    continue;
                }

                String questionTextRaw = firstCell;
                String optionA = line[1].trim();
                String optionB = line[2].trim();
                String optionC = line[3].trim();
                String optionD = line[4].trim();
                String correctAnswer = line[5].trim().toUpperCase(Locale.ROOT);

                if (questionTextRaw.isBlank()) {
                    errors.add("Row " + rowNumber + ": questionText is empty");
                    continue;
                }
                if (optionA.isBlank() || optionB.isBlank() || optionC.isBlank() || optionD.isBlank()) {
                    errors.add("Row " + rowNumber + ": one or more options are empty");
                    continue;
                }
                if (!VALID_ANSWERS.contains(correctAnswer)) {
                    errors.add("Row " + rowNumber + ": correctAnswer must be A, B, C or D");
                    continue;
                }

                Assessment assessment = fixedAssessment;
                String subject;
                String topic;
                String difficulty;

                if (fixedAssessment == null) {
                    String assessmentLookup = line[6].trim();
                    assessment = resolveAssessment(assessmentLookup);
                    if (assessment == null) {
                        errors.add("Row " + rowNumber + ": no assessment found for '" + assessmentLookup
                                + "' (expected assessment ID or exact title)");
                        continue;
                    }
                    subject = line.length > 7 ? line[7].trim() : "General";
                    topic = line.length > 8 ? line[8].trim() : "General";
                    difficulty = line.length > 9 ? line[9].trim() : "Medium";
                } else {
                    subject = line.length > 6 ? line[6].trim() : "General";
                    topic = line.length > 7 ? line[7].trim() : "General";
                    difficulty = line.length > 8 ? line[8].trim() : "Medium";
                }

                String sanitizedQuestionText = sanitizer.sanitizePlainTextPermissive(questionTextRaw);
                if (sanitizedQuestionText == null) {
                    errors.add("Row " + rowNumber + ": questionText became empty after sanitization");
                    continue;
                }

                toSave.add(Question.builder()
                        .questionText(sanitizedQuestionText)
                        .optionA(sanitizer.sanitizePlainText(optionA))
                        .optionB(sanitizer.sanitizePlainText(optionB))
                        .optionC(sanitizer.sanitizePlainText(optionC))
                        .optionD(sanitizer.sanitizePlainText(optionD))
                        .correctAnswer(sanitizer.sanitizePlainText(correctAnswer))
                        .subject(sanitizer.sanitizePlainText(subject))
                        .topic(sanitizer.sanitizePlainText(topic))
                        .difficulty(sanitizer.sanitizePlainText(difficulty))
                        .assessment(assessment)
                        .build());
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException("CSV parse error: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Failed to process CSV: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("Upload failed. Fix these issues and try again:\n" + String.join("\n", errors));
        }
        if (toSave.isEmpty()) {
            throw new RuntimeException("Upload failed. No valid question rows were found in the CSV.");
        }

        return questionRepo.saveAll(toSave);
    }

    private void validateCsvFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String fileNameLower = originalFilename == null ? "" : originalFilename.toLowerCase(Locale.ROOT);
        String contentTypeLower = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

        boolean isCsv = contentTypeLower.contains("csv")
                || contentTypeLower.contains("excel")
                || fileNameLower.endsWith(".csv");

        if (!isCsv) {
            throw new RuntimeException("Only .csv files are accepted. Received: " + contentType);
        }
    }

    private Assessment resolveAssessment(String assessmentLookup) {
        try {
            Long assessmentId = Long.parseLong(assessmentLookup);
            return assessmentRepo.findById(assessmentId).orElse(null);
        } catch (NumberFormatException e) {
            return assessmentRepo.findFirstByTitleIgnoreCase(assessmentLookup).orElse(null);
        }
    }

    private String stripBom(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "");
    }
}
