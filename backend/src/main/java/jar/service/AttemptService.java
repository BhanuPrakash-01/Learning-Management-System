package jar.service;

import jar.dto.AnswerRequest;
import jar.entity.Attempt;

import java.util.List;

public interface AttemptService {

    Attempt startAttempt(Long assessmentId, String email);

    void saveAnswer(Long attemptId, AnswerRequest request);

    Attempt submitAttempt(Long attemptId);

    List<Attempt> getMyAttempts(String email);
}