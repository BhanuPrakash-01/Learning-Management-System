package jar.dto;

import lombok.Data;

@Data
public class PracticeAttemptRequest {
    private Long questionId;
    private String selectedAnswer;
}
