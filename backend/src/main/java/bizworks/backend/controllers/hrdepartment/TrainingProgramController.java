package bizworks.backend.controllers.hrdepartment;

import bizworks.backend.dtos.hrdepartment.TrainingProgramDTO;
import bizworks.backend.models.Employee;
import bizworks.backend.models.User;
import bizworks.backend.models.hrdepartment.AttendanceTrainingProgram;
import bizworks.backend.models.hrdepartment.TrainingProgram;
import bizworks.backend.repositories.hrdepartment.TrainingProgramRepository;
import bizworks.backend.services.UserService;
import bizworks.backend.services.humanresources.TrainingProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/training-programs")
public class TrainingProgramController {
    @Autowired
    private TrainingProgramService trainingProgramService;
    @Autowired
    private UserService userService;
@Autowired private TrainingProgramRepository trainingProgramRepository;
@GetMapping
    public ResponseEntity<List<TrainingProgramDTO>> getAllTrainingPrograms() {
        List<TrainingProgramDTO> trainingPrograms = trainingProgramService.getAllTrainingPrograms();
        return ResponseEntity.ok(trainingPrograms);
    }
    @PostMapping
    public ResponseEntity<TrainingProgramDTO> createTrainingProgram(@RequestBody TrainingProgramDTO dto) {
        TrainingProgramDTO createdProgramDTO = trainingProgramService.createTrainingProgram(dto);
        return ResponseEntity.status(201).body(createdProgramDTO);
    }
    @GetMapping("/{id}")
    public ResponseEntity<TrainingProgramDTO> getTrainingProgramById(@PathVariable Long id) {
        TrainingProgramDTO dto = trainingProgramService.getTrainingProgramById(id);
        return ResponseEntity.ok(dto);
    }
    @GetMapping("/leaders")
    public ResponseEntity<List<User>> getLeaders() {
        return ResponseEntity.ok(trainingProgramService.getLeaders());
    }

    @GetMapping("/managers")
    public ResponseEntity<List<User>> getManagers() {
        List<User> managers = userService.findByRole("MANAGE");
        return ResponseEntity.ok(managers);
    }
    @GetMapping("/attendance/employee/{employeeId}")
    public ResponseEntity<List<AttendanceTrainingProgram>> getAttendanceByEmployeeId(@PathVariable Long employeeId) {
        List<AttendanceTrainingProgram> attendanceList = trainingProgramService.getAttendanceByEmployeeId(employeeId);
        return ResponseEntity.ok(attendanceList);
    }
    @PutMapping("/{id}")
    public ResponseEntity<TrainingProgramDTO> updateTrainingProgram(@PathVariable Long id, @RequestBody TrainingProgramDTO dto) {
        TrainingProgramDTO updatedProgramDTO = trainingProgramService.updateTrainingProgram(id, dto);
        return ResponseEntity.ok(updatedProgramDTO);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrainingProgram(@PathVariable Long id) {
        trainingProgramService.deleteTrainingProgram(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/new-employees")
    public ResponseEntity<List<Employee>> getNewEmployees() {
        return ResponseEntity.ok(trainingProgramService.getNewEmployees());
    }
    @PostMapping("/{programId}/attendance/{employeeId}")
    public ResponseEntity<Void> recordAttendance(
            @PathVariable Long programId,
            @PathVariable Long employeeId,
            @RequestParam String attendanceDate) {
        LocalDate date = LocalDate.parse(attendanceDate);
        trainingProgramService.recordAttendance(programId, employeeId, date);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/{programId}/attendance")
    public ResponseEntity<List<AttendanceTrainingProgram>> getAttendance(@PathVariable Long programId) {
        List<AttendanceTrainingProgram> attendanceList = trainingProgramService.getAttendanceByProgramId(programId);
        return ResponseEntity.ok(attendanceList);
    }
    @GetMapping("/employee/{employeeId}/enrollment")
    public ResponseEntity<Boolean> isEmployeeEnrolled(@PathVariable Long employeeId) {
        boolean enrolled = trainingProgramService.isEmployeeCurrentlyEnrolled(employeeId);
        return ResponseEntity.ok(enrolled);
    }
    @PutMapping("/{id}/complete")
    public ResponseEntity<Void> completeTrainingProgram(@PathVariable Long id) {
        trainingProgramService.completeTrainingProgram(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/completed")
    public ResponseEntity<List<TrainingProgramDTO>> getCompletedTrainingPrograms() {
        List<TrainingProgramDTO> completedPrograms = trainingProgramService.getCompletedTrainingPrograms();
        return ResponseEntity.ok(completedPrograms);
    }
    @GetMapping("/uncompleted")
    public ResponseEntity<List<TrainingProgramDTO>> getUncompletedTrainingPrograms() {
        List<TrainingProgramDTO> uncompletedPrograms = trainingProgramService.getUncompletedTrainingPrograms();
        return ResponseEntity.ok(uncompletedPrograms);
    }

}