package bizworks.backend.repositories.hrdepartment;

import bizworks.backend.models.hrdepartment.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
}
