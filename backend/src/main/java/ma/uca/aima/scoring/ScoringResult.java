package ma.uca.aima.scoring;

import java.math.BigDecimal;
import java.util.List;
import ma.uca.aima.referential.model.Effort;

/** Résultat complet du calcul d'une évaluation. */
public record ScoringResult(Score global, int answered, int notApplicable, int total,
                            BigDecimal completionPercent, List<DomainResult> domains,
                            List<RecommendationItem> recommendations) {

    /**
     * Un score (1,00 à 5,00), le niveau de maturité correspondant et le pourcentage (0 à 100 %).
     */
    public record Score(BigDecimal value, int level, String levelName, BigDecimal percent) {
    }

    /**
     * @param rawScore  score avant application de la règle du maillon faible
     * @param cappedBy  code de la question critique qui a plafonné le domaine (null si aucun plafond)
     * @param gap       écart à la cible : max(0, cible − score)
     */
    public record DomainResult(String code, String name, BigDecimal weight, Score score, BigDecimal rawScore,
                               String cappedBy, BigDecimal cap, int targetLevel, BigDecimal gap,
                               List<SubdomainResult> subdomains) {
    }

    public record SubdomainResult(String code, String name, Score score, int answered, int notApplicable,
                                  int total) {
    }

    /**
     * Recommandation générée pour une question dont le niveau est inférieur à la cible du domaine.
     *
     * @param action          action pour passer du niveau actuel au niveau suivant
     * @param followUpActions actions suivantes jusqu'à atteindre la cible
     */
    public record RecommendationItem(String questionCode, String questionText, String domainCode,
                                     String subdomainCode, int currentLevel, int targetLevel, int criticality,
                                     Effort effort, BigDecimal priority, Horizon horizon, String action,
                                     List<String> followUpActions) {
    }
}
