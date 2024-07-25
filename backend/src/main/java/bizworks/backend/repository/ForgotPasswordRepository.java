package aptech.project.repository;

import aptech.project.models.ForgotPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword, Long> {
    Optional<ForgotPassword> findForgotPasswordByUserId(Long user_id);
}