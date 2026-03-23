package jar.repository;

import jar.entity.CodingProblem;
import jar.entity.CodingSubmission;
import jar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodingSubmissionRepository extends JpaRepository<CodingSubmission, Long> {
    List<CodingSubmission> findByStudent(User student);

    List<CodingSubmission> findByStudentAndProblem(User student, CodingProblem problem);
}
