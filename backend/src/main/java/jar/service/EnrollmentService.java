package jar.service;

import jar.entity.Enrollment;
import java.util.List;

public interface EnrollmentService {

    Enrollment enroll(Long courseId, String studentEmail);

    List<Enrollment> getMyEnrollments(String studentEmail);
}