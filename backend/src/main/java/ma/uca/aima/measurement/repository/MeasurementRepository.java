package ma.uca.aima.measurement.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.uca.aima.measurement.model.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {

    /** Mesures d'une évaluation avec leur question, dans l'ordre du référentiel. */
    @Query("""
            select m from Measurement m join fetch m.question q join q.subdomain s join s.domain d
            where m.assessment.id = :assessmentId
            order by d.displayOrder, s.displayOrder, q.displayOrder""")
    List<Measurement> findByAssessmentWithQuestion(@Param("assessmentId") UUID assessmentId);

    Optional<Measurement> findByAssessmentIdAndQuestionId(UUID assessmentId, Long questionId);
}
