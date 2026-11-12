package ma.uca.aima.referential.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import ma.uca.aima.referential.model.Effort;

/**
 * Objets exposés par l'API (DTO). On n'expose jamais directement les entités JPA :
 * le contrat de l'API reste stable même si le modèle de base de données évolue.
 */
public final class ReferentialDtos {

    private ReferentialDtos() {
    }

    public record ReferentialSummary(Long id, String code, String version, String name, boolean active,
                                     Instant importedAt, long domainCount, long questionCount) {
    }

    public record ReferentialDetail(Long id, String code, String version, String name, String description,
                                    boolean active, int defaultTargetLevel,
                                    List<SourceDto> sources, List<MaturityLevelDto> maturityLevels,
                                    List<ScoringCapDto> scoringCaps, List<DomainDto> domains) {
    }

    public record SourceDto(String code, String name) {
    }

    public record MaturityLevelDto(int level, String name, BigDecimal minScore, String description) {
    }

    public record ScoringCapDto(int criticality, int answerLevel, BigDecimal domainCap) {
    }

    public record DomainDto(String code, String name, String description, BigDecimal weight, int order,
                            List<SubdomainDto> subdomains) {
    }

    public record SubdomainDto(String code, String name, String description,
                               Map<String, List<String>> references, List<QuestionDto> questions) {
    }

    public record QuestionDto(String code, String subdomainCode, String text, String evidence, int criticality,
                              Effort effort, List<QuestionLevelDto> levels) {
    }

    public record QuestionLevelDto(int level, String description, String recommendation) {
    }
}
