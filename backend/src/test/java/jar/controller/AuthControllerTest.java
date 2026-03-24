package jar.controller;

import jar.dto.LoginRequest;
import jar.dto.RegisterRequest;
import jar.entity.Role;
import jar.entity.User;
import jar.repository.UserRepository;
import jar.security.jwt.JwtService;
import jar.service.UserService;
import jar.service.auth.AccountSecurityService;
import jar.service.auth.AdminBootstrapService;
import jar.service.security.AuthProtectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthProtectionService authProtectionService;

    @Mock
    private AccountSecurityService accountSecurityService;

    @Mock
    private AdminBootstrapService adminBootstrapService;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerShouldReturnSuccessPayload() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@anurag.ac.in");
        request.setPassword("Password@123");
        request.setRollNumber("23EG106D01");
        request.setBranch("CSE");
        request.setBatchYear(2025);
        request.setSection("A");

        User user = User.builder()
                .name("Test User")
                .email("test@anurag.ac.in")
                .rollNumber("23EG106D01")
                .branch("CSE")
                .batchYear(2025)
                .section("A")
                .role(Role.STUDENT)
                .build();

        when(userService.register(any())).thenReturn(user);
        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<?> response = authController.register(request, httpRequest);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void loginShouldReturnTokenPayload() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@anurag.ac.in");
        request.setPassword("Password@123");

        User user = User.builder()
                .name("Admin")
                .email("admin@anurag.ac.in")
                .password("encoded")
                .emailVerified(true)
                .role(Role.ADMIN)
                .build();

        when(userService.findByEmail(request.getEmail())).thenReturn(user);
        when(passwordEncoder.matches("Password@123", "encoded")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("jwt-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = org.mockito.Mockito.mock(HttpServletResponse.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.isSecure()).thenReturn(false);

        ResponseEntity<?> response = authController.login(request, httpRequest, httpResponse);
        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("ADMIN", body.get("role"));
    }
}
