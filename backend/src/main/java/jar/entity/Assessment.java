package jar.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    // duration in minutes
    private int duration;

    @Column(columnDefinition = "TEXT")
    private String targetBranches; // comma-separated

    @Column(columnDefinition = "TEXT")
    private String targetBatches; // comma-separated

    @Column(columnDefinition = "TEXT")
    private String targetSections; // comma-separated

    @Column(name = "assessment_type", length = 30)
    private String assessmentType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Builder.Default
    private Boolean allowLateSubmission = false;

    @Builder.Default
    private Integer maxAttempts = 1;

    @Builder.Default
    private Boolean negativeMarking = false;

    @Builder.Default
    @Column(precision = 4, scale = 2)
    private BigDecimal penaltyFraction = new BigDecimal("0.25");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Course course;

    @Builder.Default
    private Boolean reviewAfterClose = false;

    @Builder.Default
    private Boolean active = true;
}
