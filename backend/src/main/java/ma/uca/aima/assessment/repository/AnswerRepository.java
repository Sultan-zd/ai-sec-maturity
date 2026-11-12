package ma.uca.aima.assessment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.uca.aima.assessment.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    /**
     * Toutes les réponses d'une évaluation, avec leur question (une seule requête SQL),
     * dans l'ordre du référentiel : domaine, puis sous-domaine, puis question.
     */
    @Query("""
            select a from Answer a join fetch a.question q join q.subdomain s join s.domain d
            where a.assessment.id = :assessmentId
            order by d.displayOrder, s.displayOrder, q.displayOrder""")
    List<Answer> findByAssessmentWithQuestion(@Param("assessmentId") UUID assessmentId);

    Optional<Answer> findByAssessmentIdAndQuestionId(UUID assessmentId, Long questionId);

    long countByAssessmentId(UUID assessmentId);

    long countByAssessmentIdAndReviewStatusNot(UUID assessmentId, ma.uca.aima.assessment.model.ReviewStatus status);
}
