package ma.uca.aima.referential.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ma.uca.aima.referential.api.ReferentialDtos.DomainDto;
import ma.uca.aima.referential.api.ReferentialDtos.MaturityLevelDto;
import ma.uca.aima.referential.api.ReferentialDtos.QuestionDto;
import ma.uca.aima.referential.api.ReferentialDtos.QuestionLevelDto;
import ma.uca.aima.referential.api.ReferentialDtos.ReferentialDetail;
import ma.uca.aima.referential.api.ReferentialDtos.ScoringCapDto;
import ma.uca.aima.referential.api.ReferentialDtos.SourceDto;
import ma.uca.aima.referential.api.ReferentialDtos.SubdomainDto;
import ma.uca.aima.referential.model.Domain;
import ma.uca.aima.referential.model.Question;
import ma.uca.aima.referential.model.Referential;
import ma.uca.aima.referential.model.Subdomain;
import ma.uca.aima.referential.model.SubdomainReference;

/** Conversion entités JPA → DTO. À appeler dans une transaction (collections chargées à la demande). */
public final class ReferentialMapper {

    private ReferentialMapper() {
    }

    public static ReferentialDetail toDetail(Referential r) {
        return new ReferentialDetail(
                r.getId(), r.getCode(), r.getVersion(), r.getName(), r.getDescription(), r.isActive(),
                r.getDefaultTargetLevel(),
                r.getSources().stream().map(s -> new SourceDto(s.getCode(), s.getName())).toList(),
                r.getMaturityLevels().stream()
                        .map(l -> new MaturityLevelDto(l.getLevel(), l.getName(), l.getMinScore(), l.getDescription()))
                        .toList(),
                r.getScoringCaps().stream()
                        .map(c -> new ScoringCapDto(c.getCriticality(), c.getAnswerLevel(), c.getDomainCap()))
                        .toList(),
                r.getDomains().stream().map(ReferentialMapper::toDomain).toList());
    }

    private static DomainDto toDomain(Domain d) {
        return new DomainDto(d.getCode(), d.getName(), d.getDescription(), d.getWeight(), d.getDisplayOrder(),
                d.getSubdomains().stream().map(ReferentialMapper::toSubdomain).toList());
    }

    private static SubdomainDto toSubdomain(Subdomain sd) {
        return new SubdomainDto(sd.getCode(), sd.getName(), sd.getDescription(),
                groupReferences(sd.getReferences()),
                sd.getQuestions().stream().map(ReferentialMapper::toQuestion).toList());
    }

    public static QuestionDto toQuestion(Question q) {
        return new QuestionDto(q.getCode(), q.getSubdomain().getCode(), q.getText(), q.getEvidence(),
                q.getCriticality(), q.getEffort(),
                q.getLevels().stream()
                        .map(l -> new QuestionLevelDto(l.getLevel(), l.getDescription(), l.getRecommendation()))
                        .toList());
    }

    /** Regroupe les références par norme : {"ISO42001": ["5.2", "A.2.2"], ...}. */
    private static Map<String, List<String>> groupReferences(List<SubdomainReference> references) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (SubdomainReference ref : references) {
            grouped.computeIfAbsent(ref.getSourceCode(), k -> new java.util.ArrayList<>()).add(ref.getClause());
        }
        return grouped;
    }
}
