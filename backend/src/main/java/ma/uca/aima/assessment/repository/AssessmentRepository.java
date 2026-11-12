package ma.uca.aima.assessment.repository;

import java.util.List;
import java.util.UUID;
import ma.uca.aima.assessment.model.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    List<Assessment> findAllByOrderByCreatedAtDesc();

    List<Assessment> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
