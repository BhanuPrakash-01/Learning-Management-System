package jar.dto;

import lombok.Data;

@Data
public class AdminBootstrapRequest {
    private String name;
    private String email;
    private String password;
}
