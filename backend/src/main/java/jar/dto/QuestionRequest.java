package jar.dto;

import lombok.Data;

@Data
public class QuestionRequest {

    private String questionText;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private String correctAnswer;

    private String subject;

    private String topic;

    private String difficulty;

    private Long assessmentId;
}
