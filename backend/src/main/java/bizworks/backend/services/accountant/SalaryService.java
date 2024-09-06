package bizworks.backend.services.accountant;

import bizworks.backend.dtos.EmployeeDTO;
import bizworks.backend.dtos.SalaryDTO;
import bizworks.backend.helpers.ApiResponse;
import bizworks.backend.models.Employee;
import bizworks.backend.models.Salary;
import bizworks.backend.models.Violation;
import bizworks.backend.repositories.EmployeeRepository;
import bizworks.backend.repositories.SalaryRepository;
import bizworks.backend.repositories.ViolationRepository;
import bizworks.backend.services.EmployeeService;
import bizworks.backend.services.MailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SalaryService {

    private final SalaryRepository salaryRepository;
    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final ViolationRepository violationRepository;
    private final MailService mailService;

    public SalaryService(SalaryRepository salaryRepository, EmployeeService employeeService,
                         EmployeeRepository employeeRepository, ViolationRepository violationRepository,
                         MailService mailService) { // Inject MailService qua constructor
        this.salaryRepository = salaryRepository;
        this.employeeRepository = employeeRepository;
        this.violationRepository = violationRepository;
        this.employeeService = employeeService;
        this.mailService = mailService;
    }

    @Scheduled(cron = "0 0 0 1 * ?")  // Chạy vào đầu mỗi tháng lúc 00:00
    public void checkAndCreateSalariesForEmployees() {
        // Lấy thông tin tháng và năm hiện tại
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // Lấy danh sách tất cả các employee
        List<Employee> employees = employeeRepository.findAll();

        for (Employee employee : employees) {
            // Kiểm tra xem bảng lương đã tồn tại cho nhân viên này trong tháng và năm hiện tại chưa
            boolean exists = salaryRepository.existsByEmployeeIdAndMonthAndYear(employee.getId(), currentMonth, currentYear);

            if (!exists) {
                // Tạo salary mới nếu chưa tồn tại
                createSalaryForEmployee(employee);
            }
        }
    }

    public EmployeeDTO getEmployeeWithAutoSalaryCheck(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // Tự động kiểm tra và tạo lương nếu chưa tồn tại
        createSalaryForEmployeeIfNotExists(employee);

        // Trả về thông tin nhân viên
        return employeeService.convertToEmpDTO(employee);
    }

    public void createSalaryForEmployeeIfNotExists(Employee employee) {
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        boolean exists = salaryRepository.existsByEmployeeIdAndMonthAndYear(employee.getId(), currentMonth, currentYear);
        if (!exists) {
            createSalaryForEmployee(employee);
        }
    }

    public void createMonthlySalariesForAllEmployees() {
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // Lấy danh sách tất cả nhân viên
        List<Employee> employees = employeeRepository.findAll();

        // Duyệt qua từng nhân viên và tạo lương nếu chưa tồn tại bản ghi lương cho tháng và năm hiện tại
        for (Employee employee : employees) {
            boolean exists = salaryRepository.existsByEmployeeIdAndMonthAndYear(employee.getId(), currentMonth, currentYear);
            if (!exists) {
                createSalaryForEmployee(employee);
            }
        }
    }
    public void createSalaryForEmployee(Employee employee) {
        // Lấy thông tin hiện tại
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // Kiểm tra xem bảng lương cho nhân viên này đã tồn tại trong tháng và năm hiện tại chưa
        boolean exists = salaryRepository.existsByEmployeeIdAndMonthAndYear(employee.getId(), currentMonth, currentYear);
        if (!exists) {
            // Tạo mới Salary nếu không tồn tại bản ghi
            Salary salary = new Salary();
            String salaryCode;
            do {
                salaryCode = generateSalaryCode();
            } while (salaryRepository.existsBySalaryCode(salaryCode)); // Kiểm tra mã có tồn tại trong cơ sở dữ liệu
            salary.setSalaryCode(salaryCode); // Tạo mã tự động cho phiếu lương
            salary.setMonth(currentMonth);
            salary.setYear(currentYear);
            salary.setBasicSalary(0.0);
            salary.setBonusSalary(0.0);
            salary.setOvertimeSalary(0.0);
            salary.setAdvanceSalary(0.0);
            salary.setAllowances(0.0);

            // Tính tiền phạt từ Violation
            List<Violation> violations = violationRepository.findByEmployeeId(employee.getId());
            double totalViolationMoney = violations.stream()
                    .mapToDouble(v -> v.getViolationType().getViolationMoney())
                    .sum();
            salary.setDeductions(totalViolationMoney);

            // Tính tổng lương
            salary.setTotalSalary(calculateTotalSalary(salary));
            salary.setDateSalary(LocalDateTime.now()); // Ngày nhận lương
            salary.setCreatedAt(LocalDateTime.now()); // Ngày tạo bản ghi
            salary.setUpdatedAt(LocalDateTime.now()); // Ngày cập nhật bản ghi
            salary.setEmployee(employee);

            salaryRepository.save(salary);
        }
    }

    public ResponseEntity<ApiResponse<SalaryDTO>> createSalary(@RequestBody SalaryDTO dto) {
        // Lấy thông tin hiện tại
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // Truy vấn Employee để lấy Position và từ đó lấy basicSalary
        Employee employee = employeeRepository.findById(dto.getEmployee().getId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // Kiểm tra xem bảng lương cho nhân viên này đã tồn tại trong tháng và năm hiện tại chưa
        boolean exists = salaryRepository.existsByEmployeeIdAndMonthAndYear(employee.getId(), currentMonth, currentYear);
        if (exists) {
            ApiResponse<SalaryDTO> badRequestResponse = ApiResponse.badRequest("Employee already has a salary record for this month");
            return new ResponseEntity<>(badRequestResponse, HttpStatus.BAD_REQUEST);
        }

        // Tạo mới Salary nếu không tồn tại bản ghi
        Salary salary = new Salary();
        salary.setSalaryCode(generateSalaryCode()); // Tạo mã tự động cho phiếu lương
        salary.setMonth(currentMonth);
        salary.setYear(currentYear);
        salary.setBasicSalary(dto.getBasicSalary() != null ? dto.getBasicSalary() : 0.0);
        salary.setBonusSalary(dto.getBonusSalary() != null ? dto.getBonusSalary() : 0.0);
        salary.setOvertimeSalary(dto.getOvertimeSalary() != null ? dto.getOvertimeSalary() : 0.0);
        salary.setAdvanceSalary(dto.getAdvanceSalary() != null ? dto.getAdvanceSalary() : 0.0);
        salary.setAllowances(dto.getAllowances() != null ? dto.getAllowances() : 0.0);


        // Tính tiền phạt từ Violation
        List<Violation> violations = violationRepository.findByEmployeeId(dto.getEmployee().getId());
        double totalViolationMoney = violations.stream()
                .mapToDouble(v -> v.getViolationType().getViolationMoney())
                .sum();
        salary.setDeductions(totalViolationMoney);

        // Tính tổng lương
        salary.setTotalSalary(calculateTotalSalary(salary));
        salary.setDateSalary(LocalDateTime.now()); // Ngày nhận lương
        salary.setCreatedAt(LocalDateTime.now()); // Ngày tạo bản ghi
        salary.setUpdatedAt(LocalDateTime.now()); // Ngày cập nhật bản ghi
        salary.setEmployee(employee);

        Salary saved = salaryRepository.save(salary);
        ApiResponse<SalaryDTO> successResponse = ApiResponse.success(convertToDTO(saved), "Salary created successfully");
        sendSalaryEmail(saved, "created");

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    public ResponseEntity<ApiResponse<SalaryDTO>> updateSalary(Long id, SalaryDTO dto) {
        Optional<Salary> optional = salaryRepository.findById(id);
        if (optional.isPresent()) {
            Salary salary = optional.get();
            salary.setSalaryCode(dto.getSalaryCode());

            LocalDateTime now = LocalDateTime.now();
            salary.setMonth(now.getMonthValue());
            salary.setYear(now.getYear());

//            Employee employee = employeeRepository.findById(dto.getEmployee().getId())
//                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
//            double basicSalary = employee.getPosition().getBasicSalary();
            salary.setBasicSalary(dto.getBasicSalary() != null ? dto.getBasicSalary() : salary.getBasicSalary());

            // Cập nhật các trường với giá trị mới nếu có
            salary.setBonusSalary(dto.getBonusSalary() != null ? dto.getBonusSalary() : salary.getBonusSalary());
            salary.setOvertimeSalary(dto.getOvertimeSalary() != null ? dto.getOvertimeSalary() : salary.getOvertimeSalary());
            salary.setAdvanceSalary(dto.getAdvanceSalary() != null ? dto.getAdvanceSalary() : salary.getAdvanceSalary());
            salary.setAllowances(dto.getAllowances() != null ? dto.getAllowances() : salary.getAllowances());

            List<Violation> violations = violationRepository.findByEmployeeId(dto.getEmployee().getId());
            double totalViolationMoney = violations.stream()
                    .mapToDouble(v -> v.getViolationType().getViolationMoney())
                    .sum();
            salary.setDeductions(totalViolationMoney);

            salary.setTotalSalary(calculateTotalSalary(salary));
            salary.setUpdatedAt(now);

            Salary updated = salaryRepository.save(salary);
            ApiResponse<SalaryDTO> successResponse = ApiResponse.success(convertToDTO(updated), "Salary updated successfully");
            sendSalaryEmail(updated, "updated");

            return new ResponseEntity<>(successResponse, HttpStatus.OK);
        }
        ApiResponse<SalaryDTO> notFoundResponse = ApiResponse.notfound(null, "Salary not found");
        return new ResponseEntity<>(notFoundResponse, HttpStatus.NOT_FOUND);
    }

    public ResponseEntity<ApiResponse<Void>> deleteSalary(Long id) {
        try {
            salaryRepository.deleteById(id);
            ApiResponse<Void> successResponse = ApiResponse.success(null, "Salary deleted successfully");
            return new ResponseEntity<>(successResponse, HttpStatus.OK);
        } catch (Exception ex) {
            ApiResponse<Void> errorResponse = ApiResponse.errorServer("An unexpected error occurred", "ERROR");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<ApiResponse<List<SalaryDTO>>> getAllSalaries() {
        List<Salary> salaries = salaryRepository.findAll();
        List<SalaryDTO> salaryDTOs = salaries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        ApiResponse<List<SalaryDTO>> successResponse = ApiResponse.success(salaryDTOs, "Salaries fetched successfully");
        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    public ResponseEntity<ApiResponse<SalaryDTO>> getSalaryById(Long id) {
        Optional<Salary> salary = salaryRepository.findById(id);
        return salary.map(s -> new ResponseEntity<>(ApiResponse.success(convertToDTO(s), "Salary fetched successfully"), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(ApiResponse.notfound(null, "Salary not found"), HttpStatus.NOT_FOUND));
    }

    public ResponseEntity<ApiResponse<List<SalaryDTO>>> searchSalariesByEmployeeName(String employeeName) {
        List<Salary> salaries = salaryRepository.findByEmployeeFullnameContaining(employeeName);
        if (salaries.isEmpty()) {
            return new ResponseEntity<>(ApiResponse.notfound(null, "No salaries found for employee"), HttpStatus.NOT_FOUND);
        }
        List<SalaryDTO> salaryDTOs = salaries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(ApiResponse.success(salaryDTOs, "Salaries fetched successfully"), HttpStatus.OK);
    }

    public ResponseEntity<ApiResponse<List<SalaryDTO>>> searchSalariesByMonthAndYear(Integer month, Integer year) {
        List<Salary> salaries = salaryRepository.findByMonthAndYear(month, year);
        if (salaries.isEmpty()) {
            return new ResponseEntity<>(ApiResponse.notfound(null, "No salaries found for the specified month and year"), HttpStatus.NOT_FOUND);
        }
        List<SalaryDTO> salaryDTOs = salaries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(ApiResponse.success(salaryDTOs, "Salaries fetched successfully"), HttpStatus.OK);
    }

    public ResponseEntity<ApiResponse<List<SalaryDTO>>> searchSalariesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Salary> salaries = salaryRepository.findByDateSalaryBetween(startDate, endDate);
        if (salaries.isEmpty()) {
            return new ResponseEntity<>(ApiResponse.notfound(null, "No salaries found for the specified date range"), HttpStatus.NOT_FOUND);
        }
        List<SalaryDTO> salaryDTOs = salaries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new ResponseEntity<>(ApiResponse.success(salaryDTOs, "Salaries fetched successfully"), HttpStatus.OK);
    }

    private double calculateTotalSalary(Salary salary) {
        return salary.getBasicSalary()
                + salary.getBonusSalary()
                + salary.getOvertimeSalary()
                + salary.getAllowances()
                - salary.getDeductions()
                - salary.getAdvanceSalary();
    }

    private SalaryDTO convertToDTO(Salary salary) {
        Employee employee = salary.getEmployee();
        EmployeeDTO employeeDTO = (employee != null)
                ? new EmployeeDTO(employee.getId(), employee.getFullname(), employee.getEmail(), employee.getPhone(),
                employee.getAvatar(),
                employee.getStartDate(),
                (employee.getDepartment() != null) ? employee.getDepartment().getName() : null, // Adjust if Department is not a String
                (employee.getPosition() != null) ? employee.getPosition().getPositionName() : null        // Adjust if Position is not a String
        )
                : null;
        return new SalaryDTO(
                salary.getId(),
                salary.getSalaryCode(),
                salary.getMonth(),
                salary.getYear(),
                salary.getBasicSalary(),
                salary.getBonusSalary(),
                salary.getOvertimeSalary(),
                salary.getAdvanceSalary(),
                salary.getAllowances(),
                salary.getDeductions(),
                salary.getTotalSalary(),
                salary.getDateSalary(),
                employeeDTO,
                salary.getCreatedAt(),
                salary.getUpdatedAt()
        );
    }

    private String generateSalaryCode() {
        return "SAL-" + System.currentTimeMillis();
    }

    public ResponseEntity<ApiResponse<SalaryDTO>> getSalaryBySalaryCode(String salaryCode) {
        Optional<Salary> salary = salaryRepository.findBySalaryCode(salaryCode);
        return salary.map(s -> new ResponseEntity<>(ApiResponse.success(convertToDTO(s), "Salary fetched successfully"), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(ApiResponse.notfound(null, "Salary not found"), HttpStatus.NOT_FOUND));
    }

    private void sendSalaryEmail(Salary salary, String action) {
        // Calculate total earnings
        double totalEarnings = salary.getBasicSalary() + salary.getBonusSalary()
                + salary.getOvertimeSalary() + salary.getAdvanceSalary()
                + salary.getAllowances();
        String netSalaryColor;

        // Calculate percentage of Net Salary compared to Basic Salary
        double basicSalary = salary.getBasicSalary();
        double netSalary = salary.getTotalSalary();
        double percentage = (netSalary / basicSalary) * 100;
        // Determine the color based on the comparison
        if (netSalary > basicSalary) {
            netSalaryColor = "#800080"; // Purple
        } else if (netSalary == basicSalary) {
            netSalaryColor = "#4CAF50"; // Green
        } else if (percentage == 80.0) {
            netSalaryColor = "#FFA500"; // Orange
        } else if (percentage < 80.0) {
            netSalaryColor = "#FF0000"; // Red
        } else {
            netSalaryColor = "#000000"; // Default to black if none of the conditions match
        }

        // Calculate total deductions
        double totalDeductions = salary.getDeductions() + salary.getAdvanceSalary(); // Add other deductions if needed

        String to = salary.getEmployee().getEmail();
        String subject = "Salary " + action;
        String content = "<div style=\"font-family: Arial, sans-serif; color: #333; line-height: 1.6;\">"
                + "<h2 style=\"color: #4CAF50;\">Dear " + salary.getEmployee().getFullname() + ",</h2>"
                + "<p>Your salary has been <strong>" + action + "</strong>.</p>"
                + "<h3 style=\"color: #2196F3;\">Salary Details:</h3>"

                // Earnings Table
                + "<div style=\"display: inline-block; width: 48%; vertical-align: top;\">"
                + "<h3 style=\"color: #4CAF50;\">Earnings</h3>"
                + "<table style=\"width: 100%; border-collapse: collapse;\">"
                + "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Basic Salary:</strong></td>"
                + "<td style=\"padding: 8px; border: 1px solid #ddd;\">" + salary.getBasicSalary()+"$" + "</td></tr>"
                + "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Bonus:</strong></td>"
                + "<td style=\"padding: 8px; border: 1px solid #ddd;\">" + salary.getBonusSalary()+"$" + "</td></tr>"
                + "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Overtime:</strong></td>"
                + "<td style=\"padding: 8px; border: 1px solid #ddd;\">" + salary.getOvertimeSalary()+"$" + "</td></tr>"
                + "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Allowances:</strong></td>"
                + "<td style=\"padding: 8px; border: 1px solid #ddd;\">" + salary.getAllowances()+"$" + "</td></tr>"
                + "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Total Earnings:</strong></td>"
                + "<td style=\"padding: 8px; border: 1px solid #ddd;\">" + totalEarnings+"$" + "</td></tr>"
                + "</table></div>"

                // Deductions Table
                + "<div style=\"display: inline-block; width: 48%; vertical-align: top; margin-left: 4%;\">"
                + "<h3 style=\"color: #F44336;\">Deductions</h3>"
                + "<table style=\"width: 100%; border-collapse: collapse;\">"
                + "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Tax Deducted at Source (T.D.S.):</strong></td>"
                + "<td style=\"padding: 8px; border: 1px solid #ddd;\">" + salary.getDeductions()+"$" + "</td></tr>"
                + "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Violations:</strong></td>"
                + "<td style=\"padding: 8px; border: 1px solid #ddd;\">" + salary.getDeductions()+"$" + "</td></tr>"
                + "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Advance Salary:</strong></td>"
                + "<td style=\"padding: 8px; border: 1px solid #ddd;\">" + salary.getAdvanceSalary()+"$" + "</td></tr>"
                + "<tr><td style=\"padding: 8px; border: 1px solid #ddd;\"><strong>Total Deductions:</strong></td>"
                + "<td style=\"padding: 8px; border: 1px solid #ddd;\">" + totalDeductions+"$" + "</td></tr>"
                + "</table></div>"

                // Net Salary
                + "<div style=\"clear: both; margin-top: 20px;\">"
                + "<h3 style=\"color: " + netSalaryColor + ";\">Net Salary: " + salary.getTotalSalary()+"$" + "</h3>"
                + "</div>"

                + "<p style=\"margin-top: 20px;\">Please contact the HR department for any questions.</p>"
                + "<p>Best regards,</p>"
                + "<h3 style=\"color: #999;\">Bizworks Team</h3>"
                + "</div>";

        try {
            mailService.sendEmail(to, subject, content);
        } catch (MessagingException e) {
            e.printStackTrace(); // You can handle the email sending error differently if needed
        }
    }


}