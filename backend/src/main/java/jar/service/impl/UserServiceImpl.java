package jar.service.impl;

import jar.dto.RegisterRequest;
import jar.entity.User;
import jar.repository.UserRepository;
import jar.service.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jar.entity.Role;

@Service
public class UserServiceImpl implements UserService {

    private static final String EMAIL_DOMAIN = "@anurag.ac.in";
    // Format: YYEG1SSXRR (e.g. 23EG106D01)
    // YY = start year, EG = university code, 1 = course code (B.Tech),
    // SS = stream code, X = section, RR = class roll number.
    private static final String ROLL_REGEX = "^[0-9]{2}EG1[0-9]{2}[A-E][0-9]{2}$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterRequest request) {
        validateRegistration(request);

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .rollNumber(request.getRollNumber().toUpperCase())
                .branch(request.getBranch())
                .batchYear(request.getBatchYear())
                .section(request.getSection().toUpperCase())
                .phone(request.getPhone())
                .active(true)
                .role(Role.STUDENT)
                .build();

        return userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    private void validateRegistration(RegisterRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Email is required");
        }
        if (!request.getEmail().toLowerCase().endsWith(EMAIL_DOMAIN)) {
            throw new RuntimeException("Only Anurag University email addresses are accepted");
        }
        if (userRepository.findByEmail(request.getEmail().toLowerCase()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }
        String normalizedRoll = request.getRollNumber() == null ? null : request.getRollNumber().toUpperCase();
        if (normalizedRoll == null || !normalizedRoll.matches(ROLL_REGEX)) {
            throw new RuntimeException("Roll number must match format 23EG106D01");
        }
        if (userRepository.findByRollNumber(normalizedRoll).isPresent()) {
            throw new RuntimeException("Roll number already registered");
        }
        if (request.getBranch() == null || request.getBranch().isBlank()) {
            throw new RuntimeException("Branch is required");
        }
        if (request.getBatchYear() == null || request.getBatchYear() < 2022 || request.getBatchYear() > 2027) {
            throw new RuntimeException("Batch year must be between 2022 and 2027");
        }
        if (request.getSection() == null || request.getSection().isBlank()) {
            throw new RuntimeException("Section is required");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && !request.getPhone().matches("^[0-9]{10}$")) {
            throw new RuntimeException("Phone number must be 10 digits");
        }
    }
}
