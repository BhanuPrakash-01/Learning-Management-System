package jar.service.impl;

import jar.dto.AnswerRequest;
import jar.entity.Assessment;
import jar.entity.Attempt;
import jar.entity.AttemptAnswer;
import jar.entity.Question;
import jar.entity.User;
import jar.repository.AssessmentRepository;
import jar.repository.AttemptAnswerRepository;
import jar.repository.AttemptRepository;
import jar.repository.EnrollmentRepository;
import jar.repository.QuestionRepository;
import jar.repository.UserRepository;
import jar.service.AttemptService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttemptServiceImpl implements AttemptService {

    private final AttemptRepository attemptRepo;
    private final AttemptAnswerRepository answerRepo;
    private final UserRepository userRepo;
    private final AssessmentRepository assessmentRepo;
    private final QuestionRepository questionRepo;
    private final EnrollmentRepository enrollmentRepo;

    public AttemptServiceImpl(AttemptRepository attemptRepo,
                              AttemptAnswerRepository answerRepo,
                              UserRepository userRepo,
                              AssessmentRepository assessmentRepo,
                              QuestionRepository questionRepo,
                              EnrollmentRepository enrollmentRepo) {
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
        this.assessmentRepo = assessmentRepo;
        this.questionRepo = questionRepo;
        this.enrollmentRepo = enrollmentRepo;
    }

    @Override
    public Attempt startAttempt(Long assessmentId, String email) {
        User student = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Assessment assessment = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        ensureStudentTargeting(student, assessment);
        LocalDateTime now = LocalDateTime.now();
        validateWindow(assessment, now);

        Attempt openAttempt = attemptRepo.findByStudentAndAssessmentAndSubmittedFalse(student, assessment)
                .orElse(null);
        if (openAttempt != null) {
            return openAttempt;
        }

        long completedAttempts = attemptRepo.countByStudentAndAssessmentAndSubmittedTrue(student, assessment);
        int maxAttempts = assessment.getMaxAttempts() == null ? 1 : assessment.getMaxAttempts();
        if (completedAttempts >= maxAttempts) {
            throw new RuntimeException("Maximum attempts reached");
        }

        LocalDateTime deadline = now.plusMinutes(assessment.getDuration());
        if (assessment.getEndTime() != null && deadline.isAfter(assessment.getEndTime())) {
            deadline = assessment.getEndTime();
        }
        if (Boolean.TRUE.equals(assessment.getAllowLateSubmission()) && assessment.getEndTime() != null) {
            LocalDateTime lateCutoff = assessment.getEndTime().plusMinutes(15);
            if (deadline.isAfter(lateCutoff)) {
                deadline = lateCutoff;
            }
        }

        return attemptRepo.save(Attempt.builder()
                .student(student)
                .assessment(assessment)
                .startTime(now)
                .deadline(deadline)
                .submitted(false)
                .score(0.0)
                .attemptNumber((int) completedAttempts + 1)
                .build());
    }

    @Override
    public void saveAnswer(Long attemptId, AnswerRequest request) {
        Attempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        Question question = questionRepo.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (attempt.isSubmitted()) {
            throw new RuntimeException("Attempt already submitted");
        }
        if (!question.getAssessment().getId().equals(attempt.getAssessment().getId())) {
            throw new RuntimeException("Question does not belong to this assessment");
        }
        if (LocalDateTime.now().isAfter(attempt.getDeadline())) {
            submitAttempt(attemptId);
            throw new RuntimeException("Time expired. Auto submitted.");
        }

        List<AttemptAnswer> existing = answerRepo.findByAttempt(attempt);
        AttemptAnswer current = existing.stream()
                .filter(ans -> ans.getQuestion().getId().equals(request.getQuestionId()))
                .findFirst()
                .orElse(null);

        if (current == null) {
            current = AttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedAnswer(request.getSelectedAnswer())
                    .build();
        } else {
            current.setSelectedAnswer(request.getSelectedAnswer());
        }
        answerRepo.save(current);
    }

    @Override
    public Attempt submitAttempt(Long attemptId) {
        Attempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        if (attempt.isSubmitted()) {
            return attempt;
        }

        List<Question> questions = questionRepo.findByAssessment(attempt.getAssessment());
        List<AttemptAnswer> answers = answerRepo.findByAttempt(attempt);

        int total = questions.size();
        int correct = 0;
        int wrong = 0;

        for (Question question : questions) {
            AttemptAnswer answer = answers.stream()
                    .filter(a -> a.getQuestion().getId().equals(question.getId()))
                    .findFirst()
                    .orElse(null);

            if (answer == null || answer.getSelectedAnswer() == null || answer.getSelectedAnswer().isBlank()) {
                continue;
            }
            if (answer.getSelectedAnswer().equalsIgnoreCase(question.getCorrectAnswer())) {
                correct++;
            } else {
                wrong++;
            }
        }

        int unanswered = Math.max(total - correct - wrong, 0);
        double score = correct;
        if (Boolean.TRUE.equals(attempt.getAssessment().getNegativeMarking())) {
            BigDecimal penalty = attempt.getAssessment().getPenaltyFraction() == null
                    ? new BigDecimal("0.25") : attempt.getAssessment().getPenaltyFraction();
            score = correct - (wrong * penalty.doubleValue());
            if (score < 0) {
                score = 0;
            }
        }

        attempt.setScore(score);
        attempt.setSubmitted(true);
        attempt.setEndTime(LocalDateTime.now());
        attempt.setTotalQuestions(total);
        attempt.setCorrectAnswers(correct);
        attempt.setWrongAnswers(wrong);
        attempt.setUnanswered(unanswered);
        return attemptRepo.save(attempt);
    }

    @Override
    public List<Attempt> getMyAttempts(String email) {
        User student = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return attemptRepo.findByStudent(student);
    }

    private void validateWindow(Assessment assessment, LocalDateTime now) {
        if (assessment.getStartTime() != null && now.isBefore(assessment.getStartTime())) {
            throw new RuntimeException("Assessment has not started yet");
        }
        if (assessment.getEndTime() == null) {
            return;
        }

        if (Boolean.TRUE.equals(assessment.getAllowLateSubmission())) {
            if (now.isAfter(assessment.getEndTime().plusMinutes(15))) {
                throw new RuntimeException("Assessment window closed");
            }
        } else if (now.isAfter(assessment.getEndTime())) {
            throw new RuntimeException("Assessment window closed");
        }
    }

    private void ensureStudentTargeting(User student, Assessment assessment) {
        if (!matchesTargets(assessment.getTargetBranches(), student.getBranch())) {
            throw new RuntimeException("Assessment is not assigned to your branch");
        }
        String batch = student.getBatchYear() == null ? null : String.valueOf(student.getBatchYear());
        if (!matchesTargets(assessment.getTargetBatches(), batch)) {
            throw new RuntimeException("Assessment is not assigned to your batch");
        }
        if (!matchesTargets(assessment.getTargetSections(), student.getSection())) {
            throw new RuntimeException("Assessment is not assigned to your section");
        }
        if (assessment.getCourse() != null && !enrollmentRepo.existsByStudentAndCourse(student, assessment.getCourse())) {
            throw new RuntimeException("You are not enrolled for this assessment's course");
        }
    }

    private boolean matchesTargets(String targets, String value) {
        if (targets == null || targets.isBlank()) {
            return true;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        Set<String> set = Arrays.stream(targets.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .collect(Collectors.toSet());
        return set.contains(value);
    }
}
