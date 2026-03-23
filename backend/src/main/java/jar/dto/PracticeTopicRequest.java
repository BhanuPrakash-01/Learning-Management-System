package jar.dto;

import lombok.Data;

@Data
public class PracticeTopicRequest {
    private Long categoryId;
    private String name;
    private String description;
    private String icon;
}
