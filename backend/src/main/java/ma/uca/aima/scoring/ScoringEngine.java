package ma.uca.aima.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import ma.uca.aima.referential.model.Effort;
import ma.uca.aima.scoring.ScoringModel.CapRule;
import ma.uca.aima.scoring.ScoringModel.DomainSpec;
import ma.uca.aima.scoring.ScoringModel.LevelSpec;
import ma.uca.aima.scoring.ScoringModel.QuestionSpec;
import ma.uca.aima.scoring.ScoringModel.SubdomainSpec;
import ma.uca.aima.scoring.ScoringResult.DomainResult;
import ma.uca.aima.scoring.ScoringResult.RecommendationItem;
import ma.uca.aima.scoring.ScoringResult.Score;
import ma.uca.aima.scoring.ScoringResult.SubdomainResult;
import org.springframework.stereotype.Component;

/**
 * Moteur de scoring AIMA — implémente la méthodologie de docs/02-modele-maturite.md :
 * <ol>
 *   <li>Sous-domaine : moyenne des niveaux pondérée par la criticité (N/A exclus).</li>
 *   <li>Domaine : moyenne pondérée des sous-domaines, puis règle du maillon faible.</li>
 *   <li>Global : moyenne pondérée des domaines (poids renormalisés si un domaine est entièrement N/A).</li>
 *   <li>Écarts à la cible, recommandations priorisées et horizons de la feuille de route.</li>
 * </ol>
 * Classe sans état et sans accès base de données : le même calcul donne toujours le même résultat.
 */
@Component
public class ScoringEngine {

    /** Seuils de priorité utilisés pour placer une recommandation dans la feuille de route. */
    static final double HIGH_PRIORITY = 45;
    static final double MEDIUM_PRIORITY = 20;

    public ScoringResult compute(ScoringModel model, Map<String, AnswerInput> answers, Map<String, Integer> targets) {
        List<DomainResult> domainResults = new ArrayList<>();
        List<RecommendationItem> recommendations = new ArrayList<>();
        int answered = 0;
        int notApplicable = 0;
        double globalSum = 0;
        double globalWeight = 0;

        for (DomainSpec domain : model.domains()) {
            int target = targets.getOrDefault(domain.code(), model.defaultTargetLevel());
            DomainAccumulator acc = new DomainAccumulator();
            List<SubdomainResult> subResults = new ArrayList<>();

            for (SubdomainSpec sd : domain.subdomains()) {
                SubdomainResult subResult = scoreSubdomain(model, sd, answers, acc);
                subResults.add(subResult);
                answered += subResult.answered();
                notApplicable += subResult.notApplicable();
            }

            DomainResult result = buildDomainResult(model, domain, target, acc, subResults);
            domainResults.add(result);
            if (result.score() != null) {
                double weight = domain.weight().doubleValue();
                globalSum += weight * result.score().value().doubleValue();
                globalWeight += weight;
            }
            recommendations.addAll(recommend(domain, target, answers));
        }

        recommendations.sort(Comparator
                .comparing(RecommendationItem::horizon)
                .thenComparing(RecommendationItem::priority, Comparator.reverseOrder())
                .thenComparing(RecommendationItem::questionCode));

        int total = model.totalQuestions();
        Score global = globalWeight > 0 ? score(globalSum / globalWeight, model.levels()) : null;
        BigDecimal completion = total == 0 ? BigDecimal.ZERO
                : round(100.0 * (answered + notApplicable) / total);
        return new ScoringResult(global, answered, notApplicable, total, completion, domainResults, recommendations);
    }

    // ------------------------------------------------------------------ sous-domaine

    private SubdomainResult scoreSubdomain(ScoringModel model, SubdomainSpec sd, Map<String, AnswerInput> answers,
                                           DomainAccumulator acc) {
        double weightedSum = 0;
        double weightTotal = 0;
        int answered = 0;
        int notApplicable = 0;

        for (QuestionSpec q : sd.questions()) {
            AnswerInput answer = answers.get(q.code());
            if (answer == null) {
                continue;                       // pas encore répondu
            }
            if (answer.notApplicable()) {
                notApplicable++;
                continue;                       // exclu du calcul
            }
            answered++;
            weightedSum += q.weight() * answer.level();
            weightTotal += q.weight();
            acc.registerCap(model.caps(), q, answer.level());
        }

        Score score = null;
        if (weightTotal > 0) {
            double value = weightedSum / weightTotal;
            score = score(value, model.levels());
            acc.add(sd.weight().doubleValue(), value);
        }
        return new SubdomainResult(sd.code(), sd.name(), score, answered, notApplicable, sd.questions().size());
    }

    // ------------------------------------------------------------------ domaine

    private DomainResult buildDomainResult(ScoringModel model, DomainSpec domain, int target, DomainAccumulator acc,
                                           List<SubdomainResult> subResults) {
        if (acc.weight == 0) {
            return new DomainResult(domain.code(), domain.name(), domain.weight(), null, null, null, null,
                    target, null, subResults);
        }
        double raw = acc.sum / acc.weight;
        double value = raw;
        String cappedBy = null;
        BigDecimal cap = null;
        if (acc.cap != null && raw > acc.cap.doubleValue()) {
            value = acc.cap.doubleValue();
            cappedBy = acc.cappedBy;
            cap = acc.cap;
        }
        Score score = score(value, model.levels());
        BigDecimal gap = round(Math.max(0, target - score.value().doubleValue()));
        return new DomainResult(domain.code(), domain.name(), domain.weight(), score, round(raw), cappedBy, cap,
                target, gap, subResults);
    }

    /** Accumule les scores des sous-domaines et le plafond le plus strict du domaine. */
    private static final class DomainAccumulator {
        private double sum;
        private double weight;
        private BigDecimal cap;
        private String cappedBy;

        void add(double w, double value) {
            sum += w * value;
            weight += w;
        }

        void registerCap(List<CapRule> rules, QuestionSpec q, int level) {
            for (CapRule rule : rules) {
                if (rule.criticality() == q.criticality() && rule.answerLevel() == level
                        && (cap == null || rule.domainCap().compareTo(cap) < 0)) {
                    cap = rule.domainCap();
                    cappedBy = q.code();
                }
            }
        }
    }

    // ------------------------------------------------------------------ recommandations

    private List<RecommendationItem> recommend(DomainSpec domain, int target, Map<String, AnswerInput> answers) {
        List<RecommendationItem> items = new ArrayList<>();
        for (SubdomainSpec sd : domain.subdomains()) {
            for (QuestionSpec q : sd.questions()) {
                AnswerInput answer = answers.get(q.code());
                if (answer == null || answer.notApplicable() || answer.level() >= target) {
                    continue;
                }
                int level = answer.level();
                // Priorité = (cible − niveau) × criticité × poids du domaine × 100.
                // Calcul en BigDecimal (exact) : en double, 1 × 3 × 0,15 × 100 = 44,999… et non 45,
                // ce qui ferait basculer la recommandation dans le mauvais horizon.
                BigDecimal priority = BigDecimal.valueOf((long) (target - level) * q.criticality())
                        .multiply(domain.weight())
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                List<String> recos = q.recommendations();
                String action = recos.get(level - 1);
                List<String> followUps = List.copyOf(recos.subList(level, Math.min(target - 1, recos.size())));
                items.add(new RecommendationItem(q.code(), q.text(), domain.code(), sd.code(), level, target,
                        q.criticality(), q.effort(), priority, horizon(priority.doubleValue(), q, level), action,
                        followUps));
            }
        }
        return items;
    }

    static Horizon horizon(double priority, QuestionSpec q, int level) {
        if (priority >= HIGH_PRIORITY && q.effort() == Effort.LOW) {
            return Horizon.QUICK_WIN;
        }
        if ((q.criticality() == 3 && level == 1) || priority >= HIGH_PRIORITY) {
            return Horizon.SHORT_TERM;
        }
        if (priority >= MEDIUM_PRIORITY) {
            return Horizon.MEDIUM_TERM;
        }
        return Horizon.LONG_TERM;
    }

    // ------------------------------------------------------------------ utilitaires

    /** Convertit un score en niveau : niveau le plus élevé dont le score minimal est atteint. */
    static Score score(double rawValue, List<LevelSpec> levels) {
        BigDecimal value = round(rawValue);
        LevelSpec reached = levels.getFirst();
        for (LevelSpec level : levels) {
            if (value.compareTo(level.minScore()) >= 0) {
                reached = level;
            }
        }
        BigDecimal percent = round((value.doubleValue() - 1) / 4 * 100);
        return new Score(value, reached.level(), reached.name(), percent);
    }

    static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
