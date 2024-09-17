    package bizworks.backend.services.humanresources;

    import bizworks.backend.dtos.hrdepartment.JobApplicationDTO;
    import bizworks.backend.models.hrdepartment.JobApplication;
    import bizworks.backend.models.hrdepartment.JobPosting;
    import bizworks.backend.repositories.hrdepartment.JobApplicationRepository;
    import bizworks.backend.repositories.hrdepartment.JobPostingRepository;
    import bizworks.backend.repositories.hrdepartment.RejectedJobApplicationRepository;
    import bizworks.backend.services.EmailService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.core.io.FileSystemResource;
    import org.springframework.core.io.Resource;
    import org.springframework.stereotype.Service;
    import org.springframework.web.multipart.MultipartFile;

    import java.io.FileNotFoundException;
    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.time.LocalDate;
    import java.util.Arrays;
    import java.util.List;
    import java.util.Optional;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class JobApplicationService {
        private final JobApplicationRepository jobApplicationRepository;
        private final JobPostingRepository jobPostingRepository;
        private final RejectedJobApplicationRepository rejectedJobApplicationRepository;

        @Autowired
        private EmailService emailService;

        private final String uploadDir = "uploads"; // Define your directory here


        public List<JobApplicationDTO> getAllApplications() {
            System.out.println("Fetching all job applications");
            return jobApplicationRepository.findAll()
                    .stream().map(this::convertToDTO).collect(Collectors.toList());
        }
        public JobApplicationDTO submitApplication(JobApplicationDTO jobApplicationDTO) {
            JobPosting jobPosting = jobPostingRepository.findById(jobApplicationDTO.getJobPostingId())
                    .orElseThrow(() -> new RuntimeException("Job posting not found"));

            // Kiểm tra ngày hết hạn
            if (LocalDate.now().isAfter(jobPosting.getDeadline())) {
                throw new RuntimeException("Cannot submit application. The job posting has expired.");
            }

            // Kiểm tra nếu ứng viên đã nộp hồ sơ cho công việc này
            Optional<JobApplication> existingApplication = jobApplicationRepository.findByJobPostingIdAndApplicantEmail(
                    jobApplicationDTO.getJobPostingId(),
                    jobApplicationDTO.getApplicantEmail()
            );

            if (existingApplication.isPresent()) {
                throw new RuntimeException("You have already submitted an application for this job posting.");
            }

            JobApplication jobApplication = new JobApplication();
            jobApplication.setJobPosting(jobPosting);
            jobApplication.setApplicantName(jobApplicationDTO.getApplicantName());
            jobApplication.setApplicantEmail(jobApplicationDTO.getApplicantEmail());
            jobApplication.setApplicantPhone(jobApplicationDTO.getApplicantPhone());
            jobApplication.setResumeUrl(jobApplicationDTO.getResumeUrl());
            jobApplication.setApplicationDate(LocalDate.now());
            jobApplication.setStatus("PENDING");

            jobApplication = jobApplicationRepository.save(jobApplication);
            return convertToDTO(jobApplication);
        }

        public Resource loadFileAsResource(String fileName) {
            try {
                Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
                Resource resource = new FileSystemResource(filePath.toFile());
                if (resource.exists()) {
                    return resource;
                } else {
                    throw new FileNotFoundException("File not found " + fileName);
                }
            } catch (Exception ex) {
                throw new RuntimeException("File not found " + fileName, ex);
            }
        }

        public String storeFile(MultipartFile file) throws IOException {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }

            // Kiểm tra phần mở rộng của tệp
            String[] allowedExtensions = {"pdf", "doc", "docx"};
            String fileExtension = getFileExtension(file.getOriginalFilename());

            if (Arrays.stream(allowedExtensions).noneMatch(fileExtension::equalsIgnoreCase)) {
                throw new RuntimeException("Invalid file type. Only PDF, DOC, and DOCX are allowed.");
            }

            // Kiểm tra MIME type để xác thực thêm
            String mimeType = Files.probeContentType(Paths.get(file.getOriginalFilename()));
            if (!mimeType.equals("application/pdf") &&
                    !mimeType.equals("application/msword") &&
                    !mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                throw new RuntimeException("Invalid file type. Only PDF, DOC, and DOCX are allowed.");
            }

            // Lưu tệp
            Path path = Paths.get(uploadDir, file.getOriginalFilename());
            Files.createDirectories(path.getParent()); // Ensure parent directories exist
            Files.write(path, file.getBytes());

            return file.getOriginalFilename(); // Return the file name without the directory
        }

        private String getFileExtension(String fileName) {
            if (fileName == null || fileName.isEmpty()) {
                return "";
            }
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }

        public List<JobApplicationDTO> getApplicationsByJobPostingId(Long jobPostingId) {
            return jobApplicationRepository.findByJobPostingId(jobPostingId)
                    .stream().map(this::convertToDTO).collect(Collectors.toList());
        }

        public JobApplicationDTO updateApplicationStatus(Long applicationId, String newStatus, String reason) {
            JobApplication jobApplication = jobApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            jobApplication.setStatus(newStatus);

            // Chỉ thêm lý do nếu trạng thái là "REJECTED"
            if (newStatus.equals("REJECTED") && reason != null && !reason.isEmpty()) {
                jobApplication.setRejectionReason(reason);
            }

            jobApplication = jobApplicationRepository.save(jobApplication);
            String applicantEmail = jobApplication.getApplicantEmail();
            String subject = "";
            String body = "";

            if (newStatus.equals("ACCEPTED")) {
                subject = "Application Status: ACCEPTED";
                body = "Dear " + jobApplication.getApplicantName() + ",\n\n"
                        + "Congratulations! Your job application has been accepted. We will contact you shortly with further details.";
            } else if (newStatus.equals("REJECTED")) {
                subject = "Application Status: REJECTED";
                body = "Dear " + jobApplication.getApplicantName() + ",\n\n"
                        + "We regret to inform you that your job application has been rejected. Reason: " + reason + "\n\n"
                        + "We appreciate your interest and encourage you to apply for future openings.";
            }

            // Gửi email nếu là trạng thái ACCEPTED hoặc REJECTED
            if (!subject.isEmpty()) {
                emailService.sendEmail(applicantEmail, subject, body);
            }

            return convertToDTO(jobApplication);
        }

        private JobApplicationDTO convertToDTO(JobApplication jobApplication) {
            return new JobApplicationDTO(
                    jobApplication.getId(),
                    jobApplication.getJobPosting().getId(),
                    jobApplication.getApplicantName(),
                    jobApplication.getApplicantEmail(),
                    jobApplication.getApplicantPhone(),
                    jobApplication.getResumeUrl(),
                    jobApplication.getApplicationDate(),
                    jobApplication.getStatus()
            );
        }

        public String getUploadDir() {
            return uploadDir;
        }
    }