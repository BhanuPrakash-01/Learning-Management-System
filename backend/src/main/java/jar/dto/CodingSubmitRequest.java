package jar.dto;

import lombok.Data;

@Data
public class CodingSubmitRequest {
    private Long problemId;
    private String language;
    private String code;
}
