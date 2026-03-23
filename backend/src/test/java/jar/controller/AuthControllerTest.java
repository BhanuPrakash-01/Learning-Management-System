package jar.controller;

import jar.dto.LoginRequest;
import jar.dto.RegisterRequest;
import jar.entity.Role;
import jar.entity.User;
import jar.security.jwt.JwtService;
import jar.service.UserService;
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

        ResponseEntity<?> response = authController.register(request);
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
                .role(Role.ADMIN)
                .build();

        when(userService.findByEmail(request.getEmail())).thenReturn(user);
        when(passwordEncoder.matches("Password@123", "encoded")).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("jwt-token", body.get("token"));
        assertEquals("ADMIN", body.get("role"));
    }
}
