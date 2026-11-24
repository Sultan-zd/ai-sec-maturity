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
import ma.uca.aima.assessment.model.ReviewStatus;
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

    /**
     * Décision de l'auditeur sur une réponse : CONFIRMED, ou ADJUSTED avec le niveau retenu.
     */
    public record ReviewRequest(
            @NotNull ReviewStatus decision,
            @Min(1) @Max(5) Integer auditedLevel,
            @Size(max = 2000) String auditorComment) {
    }

    // ------------------------------------------------------------------ réponses

    public record AssessmentSummary(UUID id, String title, String scope, AssessmentStatus status,
                                    UUID organizationId, String organizationName,
                                    Long referentialId, String referentialVersion,
                                    Map<String, Integer> targets,
                                    long answeredCount, int totalQuestions, BigDecimal completionPercent,
                                    long reviewedCount,
                                    BigDecimal globalScore, Integer globalLevel,
                                    Instant createdAt, Instant updatedAt, Instant submittedAt,
                                    Instant validatedAt) {
    }

    /**
     * @param effectiveLevel niveau utilisé dans le calcul (niveau audité s'il a été corrigé)
     */
    public record AnswerDto(String questionCode, Integer level, boolean notApplicable, String justification,
                            String comment, String evidence, Instant updatedAt,
                            ReviewStatus reviewStatus, Integer auditedLevel, String auditorComment,
                            Instant reviewedAt, Integer effectiveLevel) {
    }

    /**
     * @param evidenceRatePercent indice de confiance : part des réponses évaluées appuyées par une preuve
     */
    public record AssessmentResults(UUID assessmentId, AssessmentStatus status, BigDecimal evidenceRatePercent,
                                    ScoringResult scoring, ScoreComparison comparison) {
    }

    /**
     * Fourchette de scores : ce que l'organisation déclare, le score officiel, et ce qui est prouvé.
     *
     * @param declared       uniquement les réponses de l'organisation
     * @param official       source la plus fiable par question : mesuré &gt; audité &gt; déclaré (= scoring.global)
     * @param proven         officiel, mais une réponse sans preuve, sans revue et sans mesure compte au niveau 1
     * @param provenAnswers  réponses évaluées prouvées (preuve, revue de l'auditeur ou mesure)
     * @param evaluatedAnswers réponses évaluées (hors « Non applicable »)
     */
    public record ScoreComparison(ScoringResult.Score declared, ScoringResult.Score official,
                                  ScoringResult.Score proven, int provenAnswers, int evaluatedAnswers,
                                  List<DomainComparison> domains) {

        public static ScoreComparison of(ScoringResult declared, ScoringResult official, ScoringResult proven,
                                         int provenAnswers, int evaluatedAnswers) {
            List<DomainComparison> domains = new java.util.ArrayList<>();
            for (int i = 0; i < official.domains().size(); i++) {
                domains.add(new DomainComparison(official.domains().get(i).code(),
                        value(declared.domains().get(i).score()), value(official.domains().get(i).score()),
                        value(proven.domains().get(i).score())));
            }
            return new ScoreComparison(declared.global(), official.global(), proven.global(), provenAnswers,
                    evaluatedAnswers, domains);
        }

        private static BigDecimal value(ScoringResult.Score score) {
            return score == null ? null : score.value();
        }
    }

    public record DomainComparison(String code, BigDecimal declared, BigDecimal official, BigDecimal proven) {
    }

    public record Roadmap(UUID assessmentId, int totalActions, List<RoadmapPhase> phases) {
    }

    public record RoadmapPhase(Horizon horizon, String label, String period, List<RecommendationItem> actions) {
    }
}
