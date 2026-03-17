package jar.service;

import jar.dto.QuestionRequest;
import jar.entity.Question;

import java.util.List;

public interface QuestionService {

    Question addQuestion(QuestionRequest request);

    List<Question> getAll();

    List<Question> getByAssessment(Long assessmentId);

    Question updateQuestion(Long id, QuestionRequest request);

    void deleteQuestion(Long id);
}
