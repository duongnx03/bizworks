package bizworks.backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference; // Thêm chú thích này
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String empCode;
    private String fullname;
    private LocalDate dob;
    private String address;
    private String gender;
    private String email;
    private String phone;
    private String avatar;
    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonBackReference // Sử dụng @JsonBackReference để tránh vòng lặp
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    @JsonBackReference // Sử dụng @JsonBackReference để tránh vòng lặp
    private Position position;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.MERGE)
    private List<Attendance> attendances;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.MERGE)
    private List<LeaveRequest> leaveRequests;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.MERGE)
    private List<Salary> salaries;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonBackReference // Đánh dấu là bên tham chiếu
    private User user;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.MERGE)
    private List<Review> reviews;
}
