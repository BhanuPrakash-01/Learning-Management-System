package jar.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jar.dto.*;
import jar.entity.User;
import jar.repository.UserRepository;
import jar.security.jwt.JwtService;
import jar.service.auth.AccountSecurityService;
import jar.service.auth.AdminBootstrapService;
import jar.service.security.AuthProtectionService;
import jar.service.UserService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProtectionService authProtectionService;
    private final AccountSecurityService accountSecurityService;
    private final AdminBootstrapService adminBootstrapService;

    @Value("${security.require-https:false}")
    private boolean secureCookies;

    public AuthController(UserService userService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          AuthProtectionService authProtectionService,
                          AccountSecurityService accountSecurityService,
                          AdminBootstrapService adminBootstrapService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authProtectionService = authProtectionService;
        this.accountSecurityService = accountSecurityService;
        this.adminBootstrapService = adminBootstrapService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request,
                                      HttpServletRequest httpRequest) {
        authProtectionService.ensureRegisterRateLimit(resolveClientIp(httpRequest));
        User user = userService.register(request);
        accountSecurityService.createAndSendVerification(user);

        return ResponseEntity.ok(Map.of(
                "message", "Registration successful. Please verify your email before login.",
                "name", user.getName(),
                "email", user.getEmail()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        authProtectionService.ensureLoginRateLimit(resolveClientIp(httpRequest));

        String email = request.getEmail() == null ? "" : request.getEmail().toLowerCase();
        User user = userService.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("Invalid credentials");
        }

        authProtectionService.ensureAccountNotLocked(user);

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {
            authProtectionService.onFailedLogin(user);
            throw new RuntimeException("Invalid credentials");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Email not verified. Please verify your email before login.");
        }

        authProtectionService.onSuccessfulLogin(user);

        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name(), Map.of(
                "name", user.getName(),
                "rollNumber", user.getRollNumber() == null ? "" : user.getRollNumber(),
                "branch", user.getBranch() == null ? "" : user.getBranch(),
                "batchYear", user.getBatchYear() == null ? 0 : user.getBatchYear(),
                "section", user.getSection() == null ? "" : user.getSection(),
                "forcePasswordChange", Boolean.TRUE.equals(user.getForcePasswordChange())
        ));
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        setAuthCookies(httpResponse, accessToken, refreshToken, isSecureRequest(httpRequest));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("role", user.getRole().name());
        response.put("name", user.getName());
        response.put("rollNumber", user.getRollNumber());
        response.put("branch", user.getBranch());
        response.put("batchYear", user.getBatchYear());
        response.put("section", user.getSection());
        response.put("forcePasswordChange", Boolean.TRUE.equals(user.getForcePasswordChange()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request, "refresh_token");
        if (refreshToken == null || !jwtService.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name(), Map.of(
                "name", user.getName(),
                "rollNumber", user.getRollNumber() == null ? "" : user.getRollNumber(),
                "branch", user.getBranch() == null ? "" : user.getBranch(),
                "batchYear", user.getBatchYear() == null ? 0 : user.getBatchYear(),
                "section", user.getSection() == null ? "" : user.getSection(),
                "forcePasswordChange", Boolean.TRUE.equals(user.getForcePasswordChange())
        ));
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(isSecureRequest(request))
                .sameSite("Strict")
                .path("/")
                .maxAge(900)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        return ResponseEntity.ok(Map.of("message", "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        clearAuthCookies(response);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }
        User user = userRepository.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole().name(),
                "rollNumber", user.getRollNumber() == null ? "" : user.getRollNumber(),
                "branch", user.getBranch() == null ? "" : user.getBranch(),
                "batchYear", user.getBatchYear() == null ? 0 : user.getBatchYear(),
                "section", user.getSection() == null ? "" : user.getSection(),
                "forcePasswordChange", Boolean.TRUE.equals(user.getForcePasswordChange())
        ));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        accountSecurityService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody ResendVerificationRequest request) {
        accountSecurityService.resendVerification(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        accountSecurityService.createAndSendPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "If an account exists for this email, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        accountSecurityService.resetPassword(
                request.getToken(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody ChangePasswordRequest request) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
        accountSecurityService.changePassword(
                user,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/admin/bootstrap")
    public ResponseEntity<?> adminBootstrap(@RequestBody AdminBootstrapRequest request,
                                            @RequestHeader(value = "X-Admin-Bootstrap-Token", required = false) String token) {
        User admin = adminBootstrapService.bootstrapFromRequest(token, request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(Map.of(
                "message", "Admin bootstrap completed",
                "email", admin.getEmail()
        ));
    }

    private void setAuthCookies(HttpServletResponse response,
                                String accessToken,
                                String refreshToken,
                                boolean secure) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(900)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(604800)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return secureCookies || request.isSecure();
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
