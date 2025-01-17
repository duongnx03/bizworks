package bizworks.backend.controllers;

import bizworks.backend.dtos.DepartmentDTO;
import bizworks.backend.models.Department;
import bizworks.backend.services.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        try {
            List<DepartmentDTO> departments = departmentService.getAllDepartments();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> getDepartmentById(@PathVariable Long id) {
            try {
                Department department = departmentService.findById(id);
                return ResponseEntity.ok(department);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        }

    @GetMapping("/getByName")
    public ResponseEntity<Department> getDepartmentByName() {
        try {
            Department department = departmentService.findByName();
            return ResponseEntity.ok(department);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<Department> createDepartment(@RequestBody DepartmentDTO departmentDTO) {
        try {
            Department department = departmentService.createDepartment(departmentDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(department);
        } catch (Exception e) {
            e.printStackTrace(); // In ra thông tin chi tiết lỗi
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);  // Trả về lỗi chi tiết hơn
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @RequestBody DepartmentDTO departmentDTO) {
        try {
            Department department = departmentService.updateDepartment(id, departmentDTO);
            return ResponseEntity.ok(department);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Hoặc xử lý lỗi chi tiết hơn
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        try {
            departmentService.deleteDepartment(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Hoặc xử lý lỗi chi tiết hơn
        }
    }
}
