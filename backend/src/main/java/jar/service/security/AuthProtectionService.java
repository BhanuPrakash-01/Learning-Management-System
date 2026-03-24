package jar.service.security;

import jar.entity.User;
import jar.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthProtectionService {

    private static final int MAX_FAILED_LOGINS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private static final int LOGIN_IP_LIMIT = 20;
    private static final int REGISTER_IP_LIMIT = 10;

    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final Duration REGISTER_WINDOW = Duration.ofMinutes(30);

    private final UserRepository userRepository;

    private final Map<String, RateWindow> loginByIp = new ConcurrentHashMap<>();
    private final Map<String, RateWindow> registerByIp = new ConcurrentHashMap<>();

    public AuthProtectionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void ensureLoginRateLimit(String ip) {
        consume(loginByIp, "login:" + normalizeIp(ip), LOGIN_IP_LIMIT, LOGIN_WINDOW,
                "Too many login attempts from this IP. Please wait and try again.");
    }

    public void ensureRegisterRateLimit(String ip) {
        consume(registerByIp, "register:" + normalizeIp(ip), REGISTER_IP_LIMIT, REGISTER_WINDOW,
                "Too many registration attempts from this IP. Please wait and try again.");
    }

    public void ensureAccountNotLocked(User user) {
        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account is locked due to failed attempts. Try again after "
                    + user.getLockoutUntil());
        }
    }

    public void onFailedLogin(User user) {
        int failures = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        failures += 1;
        user.setFailedLoginAttempts(failures);
        if (failures >= MAX_FAILED_LOGINS) {
            user.setLockoutUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);
    }

    public void onSuccessfulLogin(User user) {
        boolean changed = false;
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            changed = true;
        }
        if (user.getLockoutUntil() != null) {
            user.setLockoutUntil(null);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredRateWindows() {
        LocalDateTime now = LocalDateTime.now();
        loginByIp.entrySet().removeIf(entry -> entry.getValue().resetAt().isBefore(now));
        registerByIp.entrySet().removeIf(entry -> entry.getValue().resetAt().isBefore(now));
    }

    private void consume(Map<String, RateWindow> map,
                         String key,
                         int max,
                         Duration duration,
                         String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        map.compute(key, (k, current) -> {
            if (current == null || current.resetAt().isBefore(now)) {
                return new RateWindow(1, now.plus(duration));
            }
            return new RateWindow(current.count() + 1, current.resetAt());
        });

        RateWindow window = map.get(key);
        if (window != null && window.count() > max) {
            throw new RuntimeException(errorMessage);
        }
    }

    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip;
    }

    private record RateWindow(int count, LocalDateTime resetAt) {
    }
}
