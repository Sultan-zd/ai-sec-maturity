package ma.uca.aima.assessment.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ma.uca.aima.assessment.model.AssessmentStatus;
import ma.uca.aima.scoring.Horizon;
import ma.uca.aima.scoring.ScoringResult;
import ma.uca.aima.scoring.ScoringResult.RecommendationItem;

public final class AssessmentDtos {

    private AssessmentDtos() {
    }

    // ------------------------------------------------------------------ requêtes

    /**
     * @param targets niveaux cibles par domaine (optionnel) ; les domaines absents reçoivent la cible par défaut.
     */
    public record CreateAssessmentRequest(
            @NotNull UUID organizationId,
            @NotBlank @Size(max = 255) String title,
            @Size(max = 2000) String scope,
            Map<String, @NotNull @Min(1) @Max(5) Integer> targets) {
    }

    public record TargetsRequest(@NotEmpty Map<String, @NotNull @Min(1) @Max(5) Integer> targets) {
    }

    /**
     * Réponse à une question : soit {@code level} (1 à 5), soit {@code notApplicable = true}
     * accompagné d'une {@code justification}.
     */
    public record AnswerRequest(
            @Min(1) @Max(5) Integer level,
            Boolean notApplicable,                 // optionnel : absent = false
            @Size(max = 1000) String justification,
            @Size(max = 2000) String comment,
            @Size(max = 1000) String evidence) {

        public boolean isNotApplicable() {
            return Boolean.TRUE.equals(notApplicable);
        }
    }

    // ------------------------------------------------------------------ réponses

    public record AssessmentSummary(UUID id, String title, String scope, AssessmentStatus status,
                                    UUID organizationId, String organizationName,
                                    Long referentialId, String referentialVersion,
                                    Map<String, Integer> targets,
                                    long answeredCount, int totalQuestions, BigDecimal completionPercent,
                                    BigDecimal globalScore, Integer globalLevel,
                                    Instant createdAt, Instant updatedAt, Instant submittedAt) {
    }

    public record AnswerDto(String questionCode, Integer level, boolean notApplicable, String justification,
                            String comment, String evidence, Instant updatedAt) {
    }

    /**
     * @param evidenceRatePercent indice de confiance : part des réponses évaluées appuyées par une preuve
     */
    public record AssessmentResults(UUID assessmentId, AssessmentStatus status, BigDecimal evidenceRatePercent,
                                    ScoringResult scoring) {
    }

    public record Roadmap(UUID assessmentId, int totalActions, List<RoadmapPhase> phases) {
    }

    public record RoadmapPhase(Horizon horizon, String label, String period, List<RecommendationItem> actions) {
    }
}
