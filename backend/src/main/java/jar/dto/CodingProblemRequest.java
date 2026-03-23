package jar.dto;

import lombok.Data;

@Data
public class CodingProblemRequest {
    private String title;
    private String description;
    private String difficulty;
    private String topic;
    private String sampleInput;
    private String sampleOutput;
    private String constraintsText;
    private String hints;
    private String testCases;
}
