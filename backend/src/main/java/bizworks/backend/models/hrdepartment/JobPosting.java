package bizworks.backend.models.hrdepartment;

import bizworks.backend.models.Department;
import bizworks.backend.models.Position;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "job_postings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDate postedDate;
    private LocalDate deadline;

    @ManyToOne
    @JoinColumn(name = "department_id")
    @JsonBackReference
    private Department department;

    @ManyToOne
    @JoinColumn(name = "position_id")
    @JsonBackReference
    private Position position;

    private String location;
    private String employmentType;
    private String requirements;

    // Thêm các trường mới cho mức lương
    private Double salaryRangeMin;
    private Double salaryRangeMax;

}
