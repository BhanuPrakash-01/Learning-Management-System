package jar.controller;

import jar.service.impl.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/student/notifications")
public class StudentNotificationController {

    private final NotificationService notificationService;

    public StudentNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Map<String, Object> list(Authentication auth,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return notificationService.getMyNotifications(auth.getName(), page, size);
    }

    @PatchMapping("/{id}/read")
    public Map<String, Object> markRead(Authentication auth, @PathVariable Long id) {
        notificationService.markAsRead(id, auth.getName());
        return Map.of("message", "Notification marked as read");
    }
}
