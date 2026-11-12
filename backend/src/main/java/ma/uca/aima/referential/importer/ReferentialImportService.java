package ma.uca.aima.referential.importer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ma.uca.aima.referential.importer.ReferentialDocument.CapDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.DomainDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.LevelDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.Loaded;
import ma.uca.aima.referential.importer.ReferentialDocument.QuestionDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.Root;
import ma.uca.aima.referential.importer.ReferentialDocument.SourceDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.SubdomainDoc;
import ma.uca.aima.referential.model.Domain;
import ma.uca.aima.referential.model.Effort;
import ma.uca.aima.referential.model.MaturityLevel;
import ma.uca.aima.referential.model.Question;
import ma.uca.aima.referential.model.QuestionLevel;
import ma.uca.aima.referential.model.Referential;
import ma.uca.aima.referential.model.ScoringCap;
import ma.uca.aima.referential.model.Source;
import ma.uca.aima.referential.model.Subdomain;
import ma.uca.aima.referential.model.SubdomainReference;
import ma.uca.aima.referential.repository.ReferentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importe une version du référentiel en base.
 * Idempotent : une version déjà présente (même code + version) n'est jamais réimportée.
 */
@Service
public class ReferentialImportService {

    private static final Logger log = LoggerFactory.getLogger(ReferentialImportService.class);

    private final ReferentialLoader loader;
    private final ReferentialValidator validator;
    private final ReferentialRepository repository;

    public ReferentialImportService(ReferentialLoader loader, ReferentialValidator validator,
                                    ReferentialRepository repository) {
        this.loader = loader;
        this.validator = validator;
        this.repository = repository;
    }

    public enum Outcome { IMPORTED, ALREADY_PRESENT }

    @Transactional
    public Outcome importFromLocation() {
        Loaded loaded = loader.load();
        List<String> errors = validator.validate(loaded);
        if (!errors.isEmpty()) {
            throw new InvalidReferentialException(errors);
        }
        Root root = loaded.root();
        if (repository.existsByCodeAndVersion(root.code(), root.version())) {
            log.info("Référentiel {} v{} déjà présent en base, import ignoré", root.code(), root.version());
            return Outcome.ALREADY_PRESENT;
        }

        Referential referential = toEntity(root, loaded.questions());

        // La nouvelle version devient la version active. On désactive d'abord l'ancienne et on
        // force l'écriture (flush) pour respecter l'index unique « un seul référentiel actif ».
        repository.findByActiveTrue().ifPresent(Referential::deactivate);
        repository.flush();
        referential.activate();
        repository.save(referential);

        log.info("Référentiel {} v{} importé : {} domaines, {} questions",
                root.code(), root.version(), root.domains().size(), loaded.questions().size());
        return Outcome.IMPORTED;
    }

    private Referential toEntity(Root root, List<QuestionDoc> questionDocs) {
        var rules = root.scoringRules();
        Referential referential = new Referential(root.code(), root.version(), root.name(),
                root.description(), rules.defaultTargetLevel());

        for (SourceDoc s : root.sources()) {
            referential.addSource(new Source(s.code(), s.name()));
        }
        for (LevelDoc l : root.maturityLevels()) {
            referential.addMaturityLevel(new MaturityLevel(l.level(), l.name(), l.minScore(), l.description()));
        }
        for (CapDoc c : rules.weakestLinkCaps()) {
            referential.addScoringCap(new ScoringCap(c.criticality(), c.answerLevel(), c.domainCap()));
        }

        // Les questions sont regroupées par sous-domaine en conservant l'ordre des fichiers.
        Map<String, List<QuestionDoc>> questionsBySubdomain = questionDocs.stream()
                .collect(Collectors.groupingBy(QuestionDoc::subdomain));

        for (DomainDoc d : root.domains()) {
            Domain domain = new Domain(d.code(), d.name(), d.description(), d.weight(), d.order());
            int subdomainOrder = 1;
            for (SubdomainDoc sd : d.subdomains()) {
                Subdomain subdomain = new Subdomain(sd.code(), sd.name(), sd.description(), subdomainOrder++);
                addReferences(subdomain, sd);
                int questionOrder = 1;
                for (QuestionDoc q : questionsBySubdomain.getOrDefault(sd.code(), List.of())) {
                    subdomain.addQuestion(toQuestion(q, rules.criticalityWeights(), questionOrder++));
                }
                domain.addSubdomain(subdomain);
            }
            referential.addDomain(domain);
        }
        return referential;
    }

    private static void addReferences(Subdomain subdomain, SubdomainDoc doc) {
        if (doc.references() == null) {
            return;
        }
        int order = 1;
        for (var entry : doc.references().entrySet()) {
            for (String clause : entry.getValue()) {
                subdomain.addReference(new SubdomainReference(entry.getKey(), clause, order++));
            }
        }
    }

    private static Question toQuestion(QuestionDoc q, Map<String, Integer> criticalityWeights, int order) {
        int weight = criticalityWeights.get(String.valueOf(q.criticality()));
        Question question = new Question(q.code(), q.text(), q.evidence(), q.criticality(), weight,
                Effort.valueOf(q.effort()), order);
        for (int i = 0; i < q.levels().size(); i++) {
            String recommendation = i < q.recommendations().size() ? q.recommendations().get(i) : null;
            question.addLevel(new QuestionLevel(i + 1, q.levels().get(i), recommendation));
        }
        return question;
    }
}
