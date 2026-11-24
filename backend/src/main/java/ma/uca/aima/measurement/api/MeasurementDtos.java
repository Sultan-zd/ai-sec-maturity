package ma.uca.aima.measurement.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import ma.uca.aima.measurement.model.AttackFamily;

/** Format des échanges entre le labo red-team et la plateforme (voir docs/12-contrats-modules.md). */
public final class MeasurementDtos {

    private MeasurementDtos() {
    }

    // ------------------------------------------------------------------ requêtes (labo → plateforme)

    /**
     * Un passage de tests du labo : un lot de résultats, enregistré en entier ou pas du tout.
     *
     * @param runId      identifiant du passage côté labo (ex. « garak-2026-11-24-01 »)
     * @param tool       outil utilisé (ex. « garak 0.12 »)
     * @param measuredAt date du passage (par défaut : maintenant)
     */
    public record MeasurementBatchRequest(
            @NotBlank @Size(max = 100) String runId,
            @NotBlank @Size(max = 100) String tool,
            @PastOrPresent Instant measuredAt,
            @NotEmpty @Size(max = 100) List<@Valid @NotNull MeasurementItem> results) {
    }

    /**
     * @param measuredLevel niveau de maturité constaté par le test (1 à 5), selon les niveaux décrits
     *                      dans la question du référentiel
     * @param evidence      résumé vérifiable du résultat (ex. « 14/20 charges d'injection réussies »)
     */
    public record MeasurementItem(
            @NotBlank @Size(max = 30) String questionCode,
            @NotNull AttackFamily attackFamily,
            @NotNull @Min(1) @Max(5) Integer measuredLevel,
            @NotBlank @Size(max = 2000) String evidence,
            @Size(max = 1000) String evidenceUrl) {
    }

    // ------------------------------------------------------------------ réponses

    /**
     * Une mesure, comparée à ce que l'organisation a déclaré et à ce que l'auditeur a retenu.
     *
     * @param declaredLevel niveau déclaré par l'organisation (null si pas de réponse ou « Non applicable »)
     * @param auditedLevel  niveau retenu par l'auditeur s'il l'a corrigé
     * @param gap           écart déclaré − mesuré (positif = l'organisation s'est surévaluée)
     */
    public record MeasurementDto(String questionCode, AttackFamily attackFamily, int measuredLevel,
                                 Integer declaredLevel, Integer auditedLevel, Integer gap,
                                 String tool, String runId, String evidence, String evidenceUrl,
                                 Instant measuredAt, Instant recordedAt) {
    }

    public record MeasurementBatchResult(String runId, int recorded, List<MeasurementDto> measurements) {
    }

    /** Correspondance famille d'attaque → questions mesurées (contrat avec le labo). */
    public record AttackFamilyDto(AttackFamily code, String label, Set<String> questionCodes) {
    }
}
