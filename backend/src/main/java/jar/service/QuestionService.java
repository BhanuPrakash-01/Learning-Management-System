package jar.service;

import jar.dto.QuestionRequest;
import jar.entity.Question;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionService {

    Question addQuestion(QuestionRequest request);

    List<Question> getAll();

    List<Question> getByAssessment(Long assessmentId);

    Question updateQuestion(Long id, QuestionRequest request);

    void deleteQuestion(Long id);

    List<Question> bulkUpload(MultipartFile file) throws Exception;
}