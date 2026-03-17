package jar.dto;

import lombok.Data;

@Data
public class AssessmentRequest {

    private String title;
    private String description;
    private int duration;
    private Long courseId;
}