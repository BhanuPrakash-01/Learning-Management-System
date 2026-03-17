package jar.service.impl;

import jar.dto.AnswerRequest;
import jar.entity.*;
import jar.repository.*;
import jar.service.AttemptService;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttemptServiceImpl implements AttemptService {

    private final AttemptRepository attemptRepo;
    private final AttemptAnswerRepository answerRepo;
    private final UserRepository userRepo;
    private final AssessmentRepository assessmentRepo;
    private final QuestionRepository questionRepo;

    public AttemptServiceImpl(AttemptRepository attemptRepo,
                              AttemptAnswerRepository answerRepo,
                              UserRepository userRepo,
                              AssessmentRepository assessmentRepo,
                              QuestionRepository questionRepo) {
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
        this.assessmentRepo = assessmentRepo;
        this.questionRepo = questionRepo;
    }

    @Override
    public Attempt startAttempt(Long assessmentId, String email) {

        User student = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Assessment assessment = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        return attemptRepo.findByStudentAndAssessmentAndSubmittedFalse(student, assessment)
                .orElseGet(() -> attemptRepo.save(

                        Attempt.builder()
                                .student(student)
                                .assessment(assessment)
                                .startTime(LocalDateTime.now())
                                .deadline(LocalDateTime.now().plusMinutes(assessment.getDuration()))
                                .submitted(false)
                                .score(0)
                                .build()
                ));
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

        if (LocalDateTime.now().isAfter(attempt.getDeadline())) {
            submitAttempt(attemptId);
            throw new RuntimeException("Time expired. Auto submitted.");
        }

        AttemptAnswer answer = AttemptAnswer.builder()
                .attempt(attempt)
                .question(question)
                .selectedAnswer(request.getSelectedAnswer())
                .build();

        answerRepo.save(answer);
    }

    @Override
    public Attempt submitAttempt(Long attemptId) {

        Attempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        if (attempt.isSubmitted()) {
            return attempt;
        }

        List<AttemptAnswer> answers = answerRepo.findByAttempt(attempt);

        int score = 0;

        for (AttemptAnswer ans : answers) {
            if (ans.getSelectedAnswer()
                    .equals(ans.getQuestion().getCorrectAnswer())) {
                score++;
            }
        }

        attempt.setScore(score);
        attempt.setSubmitted(true);
        attempt.setEndTime(LocalDateTime.now());

        return attemptRepo.save(attempt);
    }

    @Override
    public List<Attempt> getMyAttempts(String email) {

        User student = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        return attemptRepo.findByStudent(student);
    }
}