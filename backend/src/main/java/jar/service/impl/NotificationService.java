package jar.service.impl;

import jar.entity.Assessment;
import jar.entity.Notification;
import jar.entity.Role;
import jar.entity.User;
import jar.repository.NotificationRepository;
import jar.repository.UserRepository;
import jar.service.AssessmentService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class NotificationService {

    private static final String TYPE_ASSESSMENT_REMINDER = "ASSESSMENT_REMINDER";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AssessmentService assessmentService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               AssessmentService assessmentService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.assessmentService = assessmentService;
    }

    public Map<String, Object> getMyNotifications(String email, int page, int size) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(Math.min(size, 50), 1);
        var result = notificationRepository.findByStudentOrderByCreatedAtDesc(
                student,
                PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending())
        );
        long unreadCount = notificationRepository.countByStudentAndReadFalse(student);

        return Map.of(
                "content", result.getContent(),
                "page", safePage,
                "size", safeSize,
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "unreadCount", unreadCount
        );
    }

    @Transactional
    public void markAsRead(Long notificationId, String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void generateUpcomingAssessmentReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime within24Hours = now.plusHours(24);

        for (User student : userRepository.findByRole(Role.STUDENT)) {
            for (Assessment assessment : assessmentService.getVisibleForStudent(student.getEmail())) {
                if (assessment.getStartTime() == null) {
                    continue;
                }
                if (assessment.getStartTime().isBefore(now) || assessment.getStartTime().isAfter(within24Hours)) {
                    continue;
                }

                String refId = String.valueOf(assessment.getId());
                boolean exists = notificationRepository.existsByStudentAndTypeAndRefId(
                        student,
                        TYPE_ASSESSMENT_REMINDER,
                        refId
                );
                if (exists) {
                    continue;
                }

                String startAt = assessment.getStartTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
                notificationRepository.save(Notification.builder()
                        .student(student)
                        .type(TYPE_ASSESSMENT_REMINDER)
                        .refId(refId)
                        .title("Upcoming assessment")
                        .message(assessment.getTitle() + " starts at " + startAt)
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .build());
            }
        }
    }
}
