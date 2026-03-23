package jar.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String name;
    private String email;
    private String password;
    private String rollNumber;
    private String branch;
    private Integer batchYear;
    private String section;
    private String phone;
}
