package jar.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssessmentRequest {

    private String title;
    private String description;
    private int duration;
    private List<String> targetBranches;
    private List<Integer> targetBatchYears;
    private List<String> targetSections;
    private String assessmentType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allowLateSubmission;
    private Integer maxAttempts;
    private Boolean negativeMarking;
    private BigDecimal penaltyFraction;
}
