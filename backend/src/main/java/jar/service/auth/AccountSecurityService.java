package jar.service.auth;

import jar.entity.EmailVerificationToken;
import jar.entity.PasswordResetToken;
import jar.entity.User;
import jar.repository.EmailVerificationTokenRepository;
import jar.repository.PasswordResetTokenRepository;
import jar.repository.UserRepository;
import jar.service.security.EmailDispatchService;
import jar.service.security.InputSanitizerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AccountSecurityService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@$!%*?&";

    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailDispatchService emailDispatchService;
    private final InputSanitizerService sanitizer;

    @Value("${frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public AccountSecurityService(EmailVerificationTokenRepository verificationTokenRepository,
                                  PasswordResetTokenRepository passwordResetTokenRepository,
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  EmailDispatchService emailDispatchService,
                                  InputSanitizerService sanitizer) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailDispatchService = emailDispatchService;
        this.sanitizer = sanitizer;
    }

    @Transactional
    public void createAndSendVerification(User user) {
        verificationTokenRepository.deleteByUser(user);

        String token = generateToken();
        EmailVerificationToken record = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        verificationTokenRepository.save(record);

        String link = frontendBaseUrl + "/verify-email?token=" + token;
        String body = "Hi " + safeName(user.getName()) + ",\n\n"
                + "Please verify your Anurag LMS account using the link below:\n"
                + link + "\n\n"
                + "This verification link expires in 24 hours.";
        emailDispatchService.sendEmail(user.getEmail(), "Verify your LMS account", body);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verification = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (verification.getUsedAt() != null) {
            throw new RuntimeException("Verification token already used");
        }
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token expired");
        }

        User user = verification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verification.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(verification);
    }

    @Transactional
    public void resendVerification(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Email is already verified");
        }
        createAndSendVerification(user);
    }

    @Transactional
    public void createAndSendPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser(user);
            String token = generateToken();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            passwordResetTokenRepository.save(resetToken);

            String link = frontendBaseUrl + "/reset-password?token=" + token;
            String body = "Hi " + safeName(user.getName()) + ",\n\n"
                    + "Use the link below to reset your LMS password:\n"
                    + link + "\n\n"
                    + "This reset link expires in 1 hour.";
            emailDispatchService.sendEmail(user.getEmail(), "Reset your LMS password", body);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }
        if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
            throw new RuntimeException("Password confirmation does not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (resetToken.getUsedAt() != null) {
            throw new RuntimeException("Reset token already used");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(false);
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        passwordResetTokenRepository.deleteByUser(user);
    }

    @Transactional
    public void changePassword(User user,
                               String currentPassword,
                               String newPassword,
                               String confirmPassword) {
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Password confirmation does not match");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }

    @Transactional
    public void adminResetPassword(User student) {
        String tempPassword = generateTemporaryPassword(12);
        student.setPassword(passwordEncoder.encode(tempPassword));
        student.setForcePasswordChange(true);
        userRepository.save(student);

        String body = "Hi " + safeName(student.getName()) + ",\n\n"
                + "Your LMS password was reset by an administrator.\n"
                + "Temporary password: " + tempPassword + "\n\n"
                + "Please sign in and change this password immediately.";
        emailDispatchService.sendEmail(student.getEmail(), "LMS password reset", body);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateTemporaryPassword(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(TEMP_PASSWORD_CHARS.length());
            builder.append(TEMP_PASSWORD_CHARS.charAt(index));
        }
        return builder.toString();
    }

    private String safeName(String name) {
        String clean = sanitizer.sanitizePlainText(name);
        return clean == null ? "Student" : clean;
    }
}
