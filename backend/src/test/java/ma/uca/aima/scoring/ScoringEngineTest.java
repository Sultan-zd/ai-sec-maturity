package ma.uca.aima.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
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
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires du moteur de scoring sur un mini-référentiel :
 * <pre>
 * Domaine A (poids 0,6)
 *   A.1 : A.1.1 (critique, effort faible) · A.1.2 (normale, effort moyen)
 *   A.2 : A.2.1 (importante, effort élevé)
 * Domaine B (poids 0,4)
 *   B.1 : B.1.1 (importante, effort moyen) · B.1.2 (importante, effort faible)
 * </pre>
 * Les valeurs attendues sont calculées à la main dans les commentaires.
 */
class ScoringEngineTest {

    private final ScoringEngine engine = new ScoringEngine();
    private final ScoringModel model = miniModel();

    @Test
    void computesCriticalityWeightedMeansAndAppliesWeakestLinkCap() {
        Map<String, AnswerInput> answers = Map.of(
                "A.1.1", AnswerInput.ofLevel(2), "A.1.2", AnswerInput.ofLevel(5),
                "A.2.1", AnswerInput.ofLevel(4),
                "B.1.1", AnswerInput.ofLevel(3), "B.1.2", AnswerInput.ofLevel(3));

        ScoringResult result = engine.compute(model, answers, Map.of());

        DomainResult a = domain(result, "A");
        // A.1 = (3×2 + 1×5) / (3+1) = 2,75 ; A.2 = 4 ; brut A = (2,75 + 4) / 2 = 3,375
        assertThat(a.subdomains().getFirst().score().value()).isEqualByComparingTo("2.75");
        assertThat(a.rawScore()).isEqualByComparingTo("3.38");
        // A.1.1 critique au niveau 2 → plafond 3,0
        assertThat(a.score().value()).isEqualByComparingTo("3.00");
        assertThat(a.cappedBy()).isEqualTo("A.1.1");
        // B = 3 ; global = 0,6×3 + 0,4×3 = 3,0 → niveau 3, 50 %
        assertThat(result.global().value()).isEqualByComparingTo("3.00");
        assertThat(result.global().level()).isEqualTo(3);
        assertThat(result.global().levelName()).isEqualTo("Défini");
        assertThat(result.global().percent()).isEqualByComparingTo("50.00");
        assertThat(result.completionPercent()).isEqualByComparingTo("100.00");
    }

    @Test
    void criticalQuestionAtLevelOneCapsDomainAtTwo() {
        Map<String, AnswerInput> answers = Map.of(
                "A.1.1", AnswerInput.ofLevel(1), "A.1.2", AnswerInput.ofLevel(5), "A.2.1", AnswerInput.ofLevel(5));

        DomainResult a = domain(engine.compute(model, answers, Map.of()), "A");

        // A.1 = (3 + 5) / 4 = 2 ; A.2 = 5 ; brut = 3,5 → plafonné à 2,0
        assertThat(a.rawScore()).isEqualByComparingTo("3.50");
        assertThat(a.score().value()).isEqualByComparingTo("2.00");
        assertThat(a.score().level()).isEqualTo(2);
        assertThat(a.cap()).isEqualByComparingTo("2.0");
    }

    @Test
    void noCapWhenRawScoreIsAlreadyBelowCap() {
        Map<String, AnswerInput> answers = Map.of(
                "A.1.1", AnswerInput.ofLevel(2), "A.1.2", AnswerInput.ofLevel(1), "A.2.1", AnswerInput.ofLevel(2));

        DomainResult a = domain(engine.compute(model, answers, Map.of()), "A");

        assertThat(a.cappedBy()).isNull();
        assertThat(a.score().value()).isEqualByComparingTo(a.rawScore());
    }

    @Test
    void notApplicableAnswersAreExcludedAndGlobalWeightsRenormalized() {
        Map<String, AnswerInput> answers = Map.of(
                "A.1.1", AnswerInput.ofLevel(4), "A.1.2", AnswerInput.ofLevel(4),
                "A.2.1", AnswerInput.ofNotApplicable(),
                "B.1.1", AnswerInput.ofNotApplicable(), "B.1.2", AnswerInput.ofNotApplicable());

        ScoringResult result = engine.compute(model, answers, Map.of());

        DomainResult a = domain(result, "A");
        assertThat(a.subdomains().get(1).score()).isNull();          // A.2 entièrement N/A
        assertThat(a.score().value()).isEqualByComparingTo("4.00");   // A = A.1 seul
        assertThat(domain(result, "B").score()).isNull();             // B entièrement N/A
        // Global = A seul (poids renormalisé 0,6 / 0,6)
        assertThat(result.global().value()).isEqualByComparingTo("4.00");
        assertThat(result.notApplicable()).isEqualTo(3);
        assertThat(result.completionPercent()).isEqualByComparingTo("100.00");
    }

    @Test
    void emptyAssessmentHasNoScore() {
        ScoringResult result = engine.compute(model, Map.of(), Map.of());

        assertThat(result.global()).isNull();
        assertThat(result.completionPercent()).isEqualByComparingTo("0.00");
        assertThat(result.recommendations()).isEmpty();
        assertThat(result.domains()).allSatisfy(d -> assertThat(d.score()).isNull());
    }

    @Test
    void levelThresholdsFollowTheReferential() {
        assertThat(ScoringEngine.score(1.0, model.levels()).level()).isEqualTo(1);
        assertThat(ScoringEngine.score(1.0, model.levels()).percent()).isEqualByComparingTo("0");
        assertThat(ScoringEngine.score(1.79, model.levels()).level()).isEqualTo(1);
        assertThat(ScoringEngine.score(1.80, model.levels()).level()).isEqualTo(2);
        assertThat(ScoringEngine.score(2.594, model.levels()).level()).isEqualTo(2);  // arrondi 2,59
        assertThat(ScoringEngine.score(2.596, model.levels()).level()).isEqualTo(3);  // arrondi 2,60
        assertThat(ScoringEngine.score(5.0, model.levels()).level()).isEqualTo(5);
        assertThat(ScoringEngine.score(5.0, model.levels()).percent()).isEqualByComparingTo("100");
    }

    @Test
    void generatesPrioritizedRecommendationsUpToTarget() {
        Map<String, AnswerInput> answers = Map.of(
                "A.1.1", AnswerInput.ofLevel(1),   // (4−1)×3×0,6×100 = 540, effort faible → quick win
                "A.1.2", AnswerInput.ofLevel(4),   // déjà à la cible
                "A.2.1", AnswerInput.ofLevel(3),   // (4−3)×2×0,6×100 = 120 → court terme
                "B.1.1", AnswerInput.ofLevel(2),   // (3−2)×2×0,4×100 = 80  → court terme
                "B.1.2", AnswerInput.ofLevel(3));  // déjà à la cible
        Map<String, Integer> targets = Map.of("A", 4, "B", 3);

        List<RecommendationItem> recos = engine.compute(model, answers, targets).recommendations();

        assertThat(recos).extracting(RecommendationItem::questionCode).containsExactly("A.1.1", "A.2.1", "B.1.1");
        RecommendationItem first = recos.getFirst();
        assertThat(first.priority()).isEqualByComparingTo("540");
        assertThat(first.horizon()).isEqualTo(Horizon.QUICK_WIN);
        assertThat(first.action()).isEqualTo("A.1.1-r1");
        assertThat(first.followUpActions()).containsExactly("A.1.1-r2", "A.1.1-r3");
        assertThat(recos.get(1).horizon()).isEqualTo(Horizon.SHORT_TERM);
        assertThat(recos.get(1).priority()).isEqualByComparingTo("120");
        assertThat(recos.get(2).priority()).isEqualByComparingTo("80");
    }

    @Test
    void horizonRules() {
        QuestionSpec criticalMedium = question("X", 3, Effort.MEDIUM);
        QuestionSpec importantMedium = question("Y", 2, Effort.MEDIUM);
        QuestionSpec importantLow = question("Z", 2, Effort.LOW);

        assertThat(ScoringEngine.horizon(50, importantLow, 2)).isEqualTo(Horizon.QUICK_WIN);
        assertThat(ScoringEngine.horizon(10, criticalMedium, 1)).isEqualTo(Horizon.SHORT_TERM);
        assertThat(ScoringEngine.horizon(50, importantMedium, 2)).isEqualTo(Horizon.SHORT_TERM);
        assertThat(ScoringEngine.horizon(30, importantMedium, 2)).isEqualTo(Horizon.MEDIUM_TERM);
        assertThat(ScoringEngine.horizon(10, importantMedium, 2)).isEqualTo(Horizon.LONG_TERM);
    }

    /**
     * Non-régression : en double, 1 × 3 × 0,15 × 100 = 44,999… ; la priorité affichée (45) doit
     * bien atteindre le seuil « élevée » et classer la recommandation en court terme.
     */
    @Test
    void priorityIsExactAtThresholdDespiteFloatingPointWeights() {
        DomainSpec domain = new DomainSpec("G", "Gouvernance", new BigDecimal("0.15"), List.of(
                new SubdomainSpec("G.1", "Politique", BigDecimal.ONE,
                        List.of(question("G.1.1", 3, Effort.MEDIUM), question("G.1.2", 1, Effort.MEDIUM)))));
        ScoringModel m = new ScoringModel(1L, 3, model.levels(), model.caps(), List.of(domain));

        RecommendationItem reco = engine.compute(m,
                Map.of("G.1.1", AnswerInput.ofLevel(2), "G.1.2", AnswerInput.ofLevel(3)), Map.of())
                .recommendations().getFirst();

        assertThat(reco.priority()).isEqualByComparingTo("45");
        assertThat(reco.horizon()).isEqualTo(Horizon.SHORT_TERM);
    }

    // ------------------------------------------------------------------ jeu de test

    private static DomainResult domain(ScoringResult result, String code) {
        return result.domains().stream().filter(d -> d.code().equals(code)).findFirst().orElseThrow();
    }

    private static QuestionSpec question(String code, int criticality, Effort effort) {
        return new QuestionSpec(code, "Question " + code + " ?", criticality, criticality, effort,
                List.of(code + "-r1", code + "-r2", code + "-r3", code + "-r4"));
    }

    private static ScoringModel miniModel() {
        List<LevelSpec> levels = List.of(
                new LevelSpec(1, "Initial", new BigDecimal("1.0")),
                new LevelSpec(2, "Reproductible", new BigDecimal("1.8")),
                new LevelSpec(3, "Défini", new BigDecimal("2.6")),
                new LevelSpec(4, "Maîtrisé", new BigDecimal("3.4")),
                new LevelSpec(5, "Optimisé", new BigDecimal("4.2")));
        List<CapRule> caps = List.of(
                new CapRule(3, 1, new BigDecimal("2.0")),
                new CapRule(3, 2, new BigDecimal("3.0")));
        DomainSpec a = new DomainSpec("A", "Domaine A", new BigDecimal("0.6"), List.of(
                new SubdomainSpec("A.1", "Sous-domaine A.1", BigDecimal.ONE,
                        List.of(question("A.1.1", 3, Effort.LOW), question("A.1.2", 1, Effort.MEDIUM))),
                new SubdomainSpec("A.2", "Sous-domaine A.2", BigDecimal.ONE,
                        List.of(question("A.2.1", 2, Effort.HIGH)))));
        DomainSpec b = new DomainSpec("B", "Domaine B", new BigDecimal("0.4"), List.of(
                new SubdomainSpec("B.1", "Sous-domaine B.1", BigDecimal.ONE,
                        List.of(question("B.1.1", 2, Effort.MEDIUM), question("B.1.2", 2, Effort.LOW)))));
        return new ScoringModel(1L, 3, levels, caps, List.of(a, b));
    }

    @SuppressWarnings("unused")
    private static Map<String, AnswerInput> mutable(Map<String, AnswerInput> source) {
        return new HashMap<>(source);
    }
}
