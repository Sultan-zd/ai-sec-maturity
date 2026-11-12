package ma.uca.aima.referential.importer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Représentation fidèle des fichiers JSON du dossier {@code referential/}.
 * Ces records ne servent qu'à la lecture ; ils sont ensuite convertis en entités JPA.
 */
public final class ReferentialDocument {

    private ReferentialDocument() {
    }

    /** Contenu de {@code referentiel-v1.json}. */
    public record Root(String code, String version, String name, String description,
                       List<SourceDoc> sources, List<LevelDoc> maturityLevels,
                       ScoringRulesDoc scoringRules, List<DomainDoc> domains) {
    }

    public record SourceDoc(String code, String name) {
    }

    public record LevelDoc(int level, String name, BigDecimal minScore, String description) {
    }

    public record ScoringRulesDoc(Map<String, Integer> criticalityWeights,
                                  List<CapDoc> weakestLinkCaps, int defaultTargetLevel) {
    }

    public record CapDoc(int criticality, int answerLevel, BigDecimal domainCap) {
    }

    public record DomainDoc(String code, int order, BigDecimal weight, String name, String description,
                            List<SubdomainDoc> subdomains) {
    }

    public record SubdomainDoc(String code, String name, String description,
                               Map<String, List<String>> references) {
    }

    /** Contenu d'un fichier {@code questions/XXX.json}. */
    public record QuestionFile(String domain, List<QuestionDoc> questions) {
    }

    public record QuestionDoc(String code, String subdomain, int criticality, String effort, String text,
                              String evidence, List<String> levels, List<String> recommendations) {
    }

    /** Le référentiel complet tel que lu sur disque : la racine + toutes les questions. */
    public record Loaded(Root root, List<QuestionDoc> questions) {
    }
}
