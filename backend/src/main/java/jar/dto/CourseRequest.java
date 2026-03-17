package jar.dto;

import lombok.Data;

@Data
public class CourseRequest {

    private String title;
    private String description;
    private String instructor;
    private int duration;
}