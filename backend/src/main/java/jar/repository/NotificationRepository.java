package jar.repository;

import jar.entity.Notification;
import jar.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByStudentOrderByCreatedAtDesc(User student, Pageable pageable);

    boolean existsByStudentAndTypeAndRefId(User student, String type, String refId);

    long countByStudentAndReadFalse(User student);
}
