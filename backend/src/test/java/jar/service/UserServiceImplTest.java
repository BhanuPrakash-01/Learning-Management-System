package jar.service;

import jar.dto.RegisterRequest;
import jar.entity.User;
import jar.repository.UserRepository;
import jar.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldRejectInvalidEmailDomain() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@gmail.com");
        request.setPassword("Password@123");
        request.setRollNumber("23EG106D01");
        request.setBranch("CSE");
        request.setBatchYear(2025);
        request.setSection("A");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.register(request));
        assertEquals("Only Anurag University email addresses are accepted", ex.getMessage());
    }

    @Test
    void shouldRegisterValidStudent() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@anurag.ac.in");
        request.setPassword("Password@123");
        request.setRollNumber("23EG106D01");
        request.setBranch("CSE");
        request.setBatchYear(2025);
        request.setSection("A");

        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.findByRollNumber(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User user = userService.register(request);

        assertEquals("test@anurag.ac.in", user.getEmail());
        assertEquals("23EG106D01", user.getRollNumber());
        assertEquals("encoded", user.getPassword());
    }
}
