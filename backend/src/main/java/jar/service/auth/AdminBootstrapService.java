package jar.service.auth;

import jar.entity.Role;
import jar.entity.User;
import jar.repository.UserRepository;
import jar.service.security.InputSanitizerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InputSanitizerService sanitizer;

    @Value("${ADMIN_BOOTSTRAP_EMAIL:}")
    private String bootstrapEmail;

    @Value("${ADMIN_BOOTSTRAP_PASSWORD:}")
    private String bootstrapPassword;

    @Value("${ADMIN_BOOTSTRAP_NAME:System Admin}")
    private String bootstrapName;

    @Value("${ADMIN_BOOTSTRAP_TOKEN:}")
    private String bootstrapToken;

    public AdminBootstrapService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 InputSanitizerService sanitizer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sanitizer = sanitizer;
    }

    public boolean hasAdmin() {
        return userRepository.existsByRole(Role.ADMIN);
    }

    @Transactional
    public User bootstrapFromEnvironmentIfNeeded() {
        if (hasAdmin()) {
            return null;
        }
        if (bootstrapEmail == null || bootstrapEmail.isBlank()
                || bootstrapPassword == null || bootstrapPassword.isBlank()) {
            return null;
        }
        return createAdmin(bootstrapName, bootstrapEmail, bootstrapPassword);
    }

    @Transactional
    public User bootstrapFromRequest(String token, String name, String email, String password) {
        if (hasAdmin()) {
            throw new RuntimeException("Admin bootstrap is disabled once an admin exists");
        }
        if (bootstrapToken == null || bootstrapToken.isBlank()) {
            throw new RuntimeException("Admin bootstrap token is not configured on server");
        }
        if (!bootstrapToken.equals(token)) {
            throw new RuntimeException("Invalid bootstrap token");
        }
        return createAdmin(name, email, password);
    }

    @Transactional
    public User promoteToAdmin(User user) {
        user.setRole(Role.ADMIN);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private User createAdmin(String rawName, String rawEmail, String rawPassword) {
        String name = sanitizer.sanitizePlainText(rawName);
        String email = rawEmail == null ? null : rawEmail.toLowerCase();
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Admin name is required");
        }
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Admin email is required");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Admin email already exists");
        }
        if (rawPassword == null || rawPassword.length() < 10) {
            throw new RuntimeException("Admin password must be at least 10 characters");
        }

        User admin = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.ADMIN)
                .active(true)
                .emailVerified(true)
                .forcePasswordChange(false)
                .build();

        return userRepository.save(admin);
    }
}
