package jar.service;

import jar.dto.QuestionRequest;
import jar.entity.Assessment;
import jar.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface QuestionService {

    Question addQuestion(QuestionRequest request);

    List<Question> getAll();

    Page<Question> getAll(Pageable pageable);

    List<Question> getByAssessment(Long assessmentId);

    Page<Question> searchLibrary(String search,
                                 Long assessmentId,
                                 String difficulty,
                                 String topic,
                                 Pageable pageable);

    Map<Long, Long> assessmentQuestionCounts();

    Question updateQuestion(Long id, QuestionRequest request);

    void deleteQuestion(Long id);

    void deleteQuestions(List<Long> ids);

    List<Question> bulkUpload(MultipartFile file);

    List<Question> bulkUpload(MultipartFile file, Assessment assessment);
}
