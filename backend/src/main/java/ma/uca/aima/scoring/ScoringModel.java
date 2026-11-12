package ma.uca.aima.scoring;

import java.math.BigDecimal;
import java.util.List;
import ma.uca.aima.referential.model.Effort;

/**
 * Vue « calcul » du référentiel : uniquement ce dont le moteur de scoring a besoin.
 * Objet immuable, indépendant de JPA — il peut donc être mis en cache et testé facilement.
 */
public record ScoringModel(Long referentialId, int defaultTargetLevel, List<LevelSpec> levels,
                           List<CapRule> caps, List<DomainSpec> domains) {

    public record LevelSpec(int level, String name, BigDecimal minScore) {
    }

    /** Règle du maillon faible : question de criticité X au niveau Y → domaine plafonné à Z. */
    public record CapRule(int criticality, int answerLevel, BigDecimal domainCap) {
    }

    public record DomainSpec(String code, String name, BigDecimal weight, List<SubdomainSpec> subdomains) {
    }

    public record SubdomainSpec(String code, String name, BigDecimal weight, List<QuestionSpec> questions) {
    }

    /**
     * @param recommendations recommandations indexées par niveau : l'élément i permet de passer
     *                        du niveau i+1 au niveau i+2 (4 éléments).
     */
    public record QuestionSpec(String code, String text, int criticality, int weight, Effort effort,
                               List<String> recommendations) {
    }

    public int totalQuestions() {
        return domains.stream()
                .flatMap(d -> d.subdomains().stream())
                .mapToInt(sd -> sd.questions().size())
                .sum();
    }
}
