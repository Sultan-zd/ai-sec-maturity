package ma.uca.aima.organization.repository;

import java.util.List;
import java.util.UUID;
import ma.uca.aima.organization.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    List<Organization> findAllByOrderByNameAsc();
}
