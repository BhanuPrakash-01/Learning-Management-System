package jar.service;

import jar.dto.RegisterRequest;
import jar.entity.User;

public interface UserService {

    User register(RegisterRequest request);

    User findByEmail(String email);
}
