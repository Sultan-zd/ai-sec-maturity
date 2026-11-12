package ma.uca.aima.referential.importer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ma.uca.aima.referential.importer.ReferentialDocument.DomainDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.LevelDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.Loaded;
import ma.uca.aima.referential.importer.ReferentialDocument.QuestionDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.Root;
import ma.uca.aima.referential.importer.ReferentialDocument.SourceDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.SubdomainDoc;
import ma.uca.aima.referential.model.Effort;
import org.springframework.stereotype.Component;

/**
 * Vérifie la cohérence métier du référentiel avant import.
 * Toutes les erreurs sont collectées (et non seulement la première) pour faciliter la correction.
 */
@Component
public class ReferentialValidator {

    static final int LEVEL_COUNT = 5;
    static final int MIN_QUESTIONS_PER_SUBDOMAIN = 2;
    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.0001");

    public List<String> validate(Loaded loaded) {
        List<String> errors = new ArrayList<>();
        Root root = loaded.root();

        if (isBlank(root.code()) || isBlank(root.version()) || isBlank(root.name())) {
            errors.add("code, version et name du référentiel sont obligatoires");
        }
        validateLevels(root.maturityLevels(), errors);
        validateScoringRules(root, errors);

        Set<String> sourceCodes = new HashSet<>();
        for (SourceDoc s : nullSafe(root.sources())) {
            sourceCodes.add(s.code());
        }

        Map<String, Integer> questionsPerSubdomain = new HashMap<>();
        validateDomains(root, sourceCodes, questionsPerSubdomain, errors);
        validateQuestions(loaded.questions(), root, questionsPerSubdomain, errors);

        questionsPerSubdomain.forEach((code, count) -> {
            if (count < MIN_QUESTIONS_PER_SUBDOMAIN) {
                errors.add(code + " : " + count + " question(s), minimum " + MIN_QUESTIONS_PER_SUBDOMAIN);
            }
        });
        return errors;
    }

    private void validateLevels(List<LevelDoc> levels, List<String> errors) {
        if (nullSafe(levels).size() != LEVEL_COUNT) {
            errors.add("Il faut exactement " + LEVEL_COUNT + " niveaux de maturité");
            return;
        }
        BigDecimal previous = null;
        for (int i = 0; i < LEVEL_COUNT; i++) {
            LevelDoc level = levels.get(i);
            if (level.level() != i + 1) {
                errors.add("Les niveaux de maturité doivent être numérotés de 1 à 5 dans l'ordre");
            }
            if (previous != null && level.minScore().compareTo(previous) <= 0) {
                errors.add("Le score minimal du niveau " + level.level() + " doit être croissant");
            }
            previous = level.minScore();
        }
    }

    private void validateScoringRules(Root root, List<String> errors) {
        var rules = root.scoringRules();
        if (rules == null) {
            errors.add("scoringRules est obligatoire");
            return;
        }
        if (rules.defaultTargetLevel() < 1 || rules.defaultTargetLevel() > LEVEL_COUNT) {
            errors.add("defaultTargetLevel doit être compris entre 1 et 5");
        }
        for (int c = 1; c <= 3; c++) {
            Integer weight = rules.criticalityWeights() == null ? null : rules.criticalityWeights().get(String.valueOf(c));
            if (weight == null || weight <= 0) {
                errors.add("criticalityWeights doit définir un poids positif pour la criticité " + c);
            }
        }
    }

    private void validateDomains(Root root, Set<String> sourceCodes,
                                 Map<String, Integer> questionsPerSubdomain, List<String> errors) {
        BigDecimal weightSum = BigDecimal.ZERO;
        Set<String> domainCodes = new HashSet<>();
        for (DomainDoc d : nullSafe(root.domains())) {
            if (!domainCodes.add(d.code())) {
                errors.add("Domaine en double : " + d.code());
            }
            weightSum = weightSum.add(d.weight());
            if (nullSafe(d.subdomains()).isEmpty()) {
                errors.add(d.code() + " : aucun sous-domaine");
            }
            for (SubdomainDoc sd : nullSafe(d.subdomains())) {
                if (questionsPerSubdomain.putIfAbsent(sd.code(), 0) != null) {
                    errors.add("Sous-domaine en double : " + sd.code());
                }
                if (!sd.code().startsWith(d.code() + ".")) {
                    errors.add(sd.code() + " : le code doit commencer par " + d.code() + ".");
                }
                if (sd.references() != null) {
                    sd.references().keySet().stream()
                            .filter(src -> !sourceCodes.contains(src))
                            .forEach(src -> errors.add(sd.code() + " : source inconnue " + src));
                }
            }
        }
        if (weightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            errors.add("La somme des poids des domaines vaut " + weightSum + " (attendu : 1)");
        }
    }

    private void validateQuestions(List<QuestionDoc> questions, Root root,
                                   Map<String, Integer> questionsPerSubdomain, List<String> errors) {
        Set<String> codes = new HashSet<>();
        for (QuestionDoc q : nullSafe(questions)) {
            String code = q.code();
            if (!codes.add(code)) {
                errors.add(code + " : code en double");
            }
            if (!questionsPerSubdomain.containsKey(q.subdomain())) {
                errors.add(code + " : sous-domaine inconnu " + q.subdomain());
            } else {
                questionsPerSubdomain.merge(q.subdomain(), 1, Integer::sum);
            }
            if (code == null || !code.startsWith(q.subdomain() + ".")) {
                errors.add(code + " : code incohérent avec le sous-domaine " + q.subdomain());
            }
            if (q.criticality() < 1 || q.criticality() > 3) {
                errors.add(code + " : criticité invalide " + q.criticality());
            }
            if (!isValidEffort(q.effort())) {
                errors.add(code + " : effort invalide " + q.effort());
            }
            if (isBlank(q.text()) || isBlank(q.evidence())) {
                errors.add(code + " : texte et preuves attendues obligatoires");
            }
            if (nullSafe(q.levels()).size() != LEVEL_COUNT) {
                errors.add(code + " : il faut " + LEVEL_COUNT + " descriptions de niveaux");
            }
            if (nullSafe(q.recommendations()).size() != LEVEL_COUNT - 1) {
                errors.add(code + " : il faut " + (LEVEL_COUNT - 1) + " recommandations");
            }
        }
    }

    private static boolean isValidEffort(String effort) {
        try {
            Effort.valueOf(effort);
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
