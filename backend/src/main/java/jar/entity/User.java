package jar.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "roll_number", unique = true, length = 15)
    private String rollNumber;

    @Column(length = 20)
    private String branch;

    @Column(name = "batch_year")
    private Integer batchYear;

    @Column(length = 5)
    private String section;

    @Column(length = 10)
    private String phone;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;  // STUDENT or ADMIN
}
