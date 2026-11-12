package ma.uca.aima.referential.repository;

import java.util.List;
import java.util.Optional;
import ma.uca.aima.referential.model.Referential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReferentialRepository extends JpaRepository<Referential, Long> {

    Optional<Referential> findByActiveTrue();

    boolean existsByCodeAndVersion(String code, String version);

    List<Referential> findAllByOrderByImportedAtDesc();

    @Query("select count(d) from Domain d where d.referential.id = :referentialId")
    long countDomains(@Param("referentialId") Long referentialId);

    @Query("select count(q) from Question q where q.subdomain.domain.referential.id = :referentialId")
    long countQuestions(@Param("referentialId") Long referentialId);
}
