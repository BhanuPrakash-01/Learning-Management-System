package jar.controller;

import jar.entity.Assessment;
import jar.entity.Attempt;
import jar.entity.AttemptAnswer;
import jar.entity.User;
import jar.repository.AssessmentRepository;
import jar.repository.AttemptAnswerRepository;
import jar.repository.AttemptRepository;
import jar.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportsController {

    private final AssessmentRepository assessmentRepo;
    private final AttemptRepository attemptRepo;
    private final AttemptAnswerRepository answerRepo;
    private final UserRepository userRepo;

    public AdminReportsController(AssessmentRepository assessmentRepo,
                                  AttemptRepository attemptRepo,
                                  AttemptAnswerRepository answerRepo,
                                  UserRepository userRepo) {
        this.assessmentRepo = assessmentRepo;
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/assessment/{id}")
    public ResponseEntity<byte[]> assessmentReport(@PathVariable Long id,
                                                   @RequestParam(required = false, defaultValue = "csv") String format) throws Exception {
        Assessment assessment = assessmentRepo.findById(id).orElseThrow(() -> new RuntimeException("Assessment not found"));
        List<Attempt> attempts = attemptRepo.findByAssessmentAndSubmittedTrue(assessment);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Attempt attempt : attempts) {
            List<AttemptAnswer> answers = answerRepo.findByAttempt(attempt);
            rows.add(Map.of(
                    "rollNumber", attempt.getStudent() == null ? "" : safe(attempt.getStudent().getRollNumber()),
                    "name", attempt.getStudent() == null ? "" : safe(attempt.getStudent().getName()),
                    "branch", attempt.getStudent() == null ? "" : safe(attempt.getStudent().getBranch()),
                    "section", attempt.getStudent() == null ? "" : safe(attempt.getStudent().getSection()),
                    "score", attempt.getScore(),
                    "correct", attempt.getCorrectAnswers(),
                    "wrong", attempt.getWrongAnswers(),
                    "timeTaken", attempt.getStartTime() == null || attempt.getEndTime() == null
                            ? "N/A" : java.time.Duration.between(attempt.getStartTime(), attempt.getEndTime()).toMinutes() + " min",
                    "answers", answers.size()
            ));
        }

        if ("xlsx".equalsIgnoreCase(format)) {
            return buildXlsxResponse(rows, "assessment_report_" + id + ".xlsx");
        }
        return buildCsvResponse(rows, "assessment_report_" + id + ".csv");
    }

    @GetMapping("/non-attempts/{id}")
    public ResponseEntity<byte[]> nonAttemptsReport(@PathVariable Long id,
                                                    @RequestParam(required = false, defaultValue = "csv") String format) throws Exception {
        Assessment assessment = assessmentRepo.findById(id).orElseThrow(() -> new RuntimeException("Assessment not found"));
        List<User> students = userRepo.findAll().stream()
                .filter(user -> user.getRole() != null && user.getRole().name().equals("STUDENT"))
                .toList();
        List<Attempt> attempts = attemptRepo.findByAssessment(assessment);

        List<Map<String, Object>> rows = students.stream()
                .filter(student -> attempts.stream().noneMatch(attempt -> attempt.getStudent() != null
                        && attempt.getStudent().getId().equals(student.getId())
                        && attempt.isSubmitted()))
                .map(student -> {
                    Map<String, Object> row = new java.util.HashMap<>();
                    row.put("rollNumber", safe(student.getRollNumber()));
                    row.put("name", safe(student.getName()));
                    row.put("branch", safe(student.getBranch()));
                    row.put("batchYear", student.getBatchYear() == null ? "" : String.valueOf(student.getBatchYear()));
                    row.put("section", safe(student.getSection()));
                    return row;
                })
                .toList();

        if ("xlsx".equalsIgnoreCase(format)) {
            return buildXlsxResponse(rows, "non_attempts_" + id + ".xlsx");
        }
        return buildCsvResponse(rows, "non_attempts_" + id + ".csv");
    }

    private ResponseEntity<byte[]> buildCsvResponse(List<Map<String, Object>> rows, String fileName) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!rows.isEmpty()) {
            List<String> headers = new ArrayList<>(rows.get(0).keySet());
            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(headers.toArray(new String[0])).build();
            try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), format)) {
                for (Map<String, Object> row : rows) {
                    List<String> values = headers.stream().map(h -> String.valueOf(row.getOrDefault(h, ""))).toList();
                    printer.printRecord(values);
                }
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(baos.toByteArray());
    }

    private ResponseEntity<byte[]> buildXlsxResponse(List<Map<String, Object>> rows, String fileName) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Report");
        if (!rows.isEmpty()) {
            List<String> headers = new ArrayList<>(rows.get(0).keySet());
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < headers.size(); c++) {
                    row.createCell(c).setCellValue(String.valueOf(rows.get(r).getOrDefault(headers.get(c), "")));
                }
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(baos.toByteArray());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
