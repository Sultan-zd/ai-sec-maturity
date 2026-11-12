package ma.uca.aima.scoring;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ma.uca.aima.referential.model.Question;
import ma.uca.aima.referential.model.QuestionLevel;
import ma.uca.aima.referential.model.Referential;
import ma.uca.aima.referential.service.ReferentialQueryService;
import ma.uca.aima.scoring.ScoringModel.CapRule;
import ma.uca.aima.scoring.ScoringModel.DomainSpec;
import ma.uca.aima.scoring.ScoringModel.LevelSpec;
import ma.uca.aima.scoring.ScoringModel.QuestionSpec;
import ma.uca.aima.scoring.ScoringModel.SubdomainSpec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Construit le {@link ScoringModel} d'une version du référentiel.
 * Une version étant immuable, le modèle est construit une seule fois puis gardé en mémoire.
 */
@Component
public class ScoringModelFactory {

    private final ReferentialQueryService referentialService;
    private final Map<Long, ScoringModel> cache = new ConcurrentHashMap<>();

    public ScoringModelFactory(ReferentialQueryService referentialService) {
        this.referentialService = referentialService;
    }

    @Transactional(readOnly = true)
    public ScoringModel forReferential(Long referentialId) {
        ScoringModel cached = cache.get(referentialId);
        if (cached != null) {
            return cached;
        }
        ScoringModel model = build(referentialService.findById(referentialId));
        cache.put(referentialId, model);
        return model;
    }

    private static ScoringModel build(Referential r) {
        List<LevelSpec> levels = r.getMaturityLevels().stream()
                .map(l -> new LevelSpec(l.getLevel(), l.getName(), l.getMinScore()))
                .toList();
        List<CapRule> caps = r.getScoringCaps().stream()
                .map(c -> new CapRule(c.getCriticality(), c.getAnswerLevel(), c.getDomainCap()))
                .toList();
        List<DomainSpec> domains = r.getDomains().stream()
                .map(d -> new DomainSpec(d.getCode(), d.getName(), d.getWeight(),
                        d.getSubdomains().stream()
                                .map(sd -> new SubdomainSpec(sd.getCode(), sd.getName(), sd.getWeight(),
                                        sd.getQuestions().stream().map(ScoringModelFactory::toSpec).toList()))
                                .toList()))
                .toList();
        return new ScoringModel(r.getId(), r.getDefaultTargetLevel(), levels, caps, domains);
    }

    private static QuestionSpec toSpec(Question q) {
        List<String> recommendations = q.getLevels().stream()
                .map(QuestionLevel::getRecommendation)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new QuestionSpec(q.getCode(), q.getText(), q.getCriticality(), q.getWeight(), q.getEffort(),
                recommendations);
    }
}
