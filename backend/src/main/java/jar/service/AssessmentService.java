package jar.service;

import jar.dto.AssessmentRequest;
import jar.entity.Assessment;

import java.util.List;

public interface AssessmentService {

    Assessment createAssessment(AssessmentRequest request);

    List<Assessment> getAll();

    Assessment getById(Long id);

    List<Assessment> getByCourse(Long courseId);

    Assessment updateAssessment(Long id, AssessmentRequest request);

    void deleteAssessment(Long id);
}