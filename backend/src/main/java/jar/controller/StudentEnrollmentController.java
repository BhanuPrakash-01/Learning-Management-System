package jar.controller;

import jar.entity.Enrollment;
import jar.service.EnrollmentService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/enrollments")
public class StudentEnrollmentController {

    private final EnrollmentService enrollmentService;

    public StudentEnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/{courseId}")
    public Enrollment enroll(@PathVariable Long courseId,
                             Authentication authentication) {

        String email = authentication.getName();
        return enrollmentService.enroll(courseId, email);
    }

    @GetMapping
    public List<Enrollment> myEnrollments(Authentication authentication) {

        String email = authentication.getName();
        return enrollmentService.getMyEnrollments(email);
    }
}