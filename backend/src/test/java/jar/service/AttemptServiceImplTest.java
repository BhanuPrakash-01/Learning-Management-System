package jar.service;

import jar.entity.Assessment;
import jar.entity.Attempt;
import jar.entity.AttemptAnswer;
import jar.entity.Question;
import jar.entity.User;
import jar.repository.AssessmentRepository;
import jar.repository.AttemptAnswerRepository;
import jar.repository.AttemptRepository;
import jar.repository.QuestionRepository;
import jar.repository.UserRepository;
import jar.service.impl.AttemptServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceImplTest {

    @Mock
    private AttemptRepository attemptRepo;
    @Mock
    private AttemptAnswerRepository answerRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private AssessmentRepository assessmentRepo;
    @Mock
    private QuestionRepository questionRepo;

    @InjectMocks
    private AttemptServiceImpl attemptService;

    @Test
    void submitAttemptShouldApplyNegativeMarking() {
        Assessment assessment = Assessment.builder()
                .id(1L)
                .duration(30)
                .negativeMarking(true)
                .penaltyFraction(new BigDecimal("0.25"))
                .maxAttempts(1)
                .build();

        Attempt attempt = Attempt.builder()
                .id(1L)
                .assessment(assessment)
                .student(User.builder().id(1L).build())
                .submitted(false)
                .startTime(LocalDateTime.now().minusMinutes(20))
                .deadline(LocalDateTime.now().plusMinutes(10))
                .build();

        Question q1 = Question.builder().id(1L).correctAnswer("A").assessment(assessment).build();
        Question q2 = Question.builder().id(2L).correctAnswer("B").assessment(assessment).build();
        Question q3 = Question.builder().id(3L).correctAnswer("C").assessment(assessment).build();

        AttemptAnswer a1 = AttemptAnswer.builder().question(q1).selectedAnswer("A").build();
        AttemptAnswer a2 = AttemptAnswer.builder().question(q2).selectedAnswer("D").build();
        AttemptAnswer a3 = AttemptAnswer.builder().question(q3).selectedAnswer("C").build();

        when(attemptRepo.findById(1L)).thenReturn(Optional.of(attempt));
        when(questionRepo.findByAssessment(assessment)).thenReturn(List.of(q1, q2, q3));
        when(answerRepo.findByAttempt(attempt)).thenReturn(List.of(a1, a2, a3));
        when(attemptRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Attempt submitted = attemptService.submitAttempt(1L);

        // 2 correct - (1 wrong * 0.25) = 1.75
        assertEquals(1.75, submitted.getScore(), 0.0001);
        assertEquals(2, submitted.getCorrectAnswers());
        assertEquals(1, submitted.getWrongAnswers());
        assertEquals(0, submitted.getUnanswered());
    }
}
