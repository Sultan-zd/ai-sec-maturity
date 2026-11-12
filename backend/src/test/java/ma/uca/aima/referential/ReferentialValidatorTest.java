package ma.uca.aima.referential;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import ma.uca.aima.referential.importer.ReferentialDocument.DomainDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.Loaded;
import ma.uca.aima.referential.importer.ReferentialDocument.QuestionDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.Root;
import ma.uca.aima.referential.importer.ReferentialLoader;
import ma.uca.aima.referential.importer.ReferentialValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Tests unitaires (sans Spring ni base de données) du validateur de référentiel. */
class ReferentialValidatorTest {

    private final ReferentialValidator validator = new ReferentialValidator();
    private Loaded official;

    @BeforeEach
    void loadOfficialReferential() {
        official = new ReferentialLoader(new PathMatchingResourcePatternResolver(), "classpath:referential").load();
    }

    @Test
    void officialReferentialIsValid() {
        assertThat(validator.validate(official)).isEmpty();
        assertThat(official.root().domains()).hasSize(8);
        assertThat(official.questions()).hasSize(59);
    }

    @Test
    void detectsDomainWeightsNotSummingToOne() {
        Root root = official.root();
        List<DomainDoc> domains = new ArrayList<>(root.domains());
        DomainDoc first = domains.getFirst();
        domains.set(0, new DomainDoc(first.code(), first.order(), new BigDecimal("0.50"), first.name(),
                first.description(), first.subdomains()));
        Root broken = new Root(root.code(), root.version(), root.name(), root.description(), root.sources(),
                root.maturityLevels(), root.scoringRules(), domains);

        assertThat(validator.validate(new Loaded(broken, official.questions())))
                .anySatisfy(error -> assertThat(error).contains("somme des poids"));
    }

    @Test
    void detectsQuestionWithMissingLevelAndUnknownSubdomain() {
        List<QuestionDoc> questions = new ArrayList<>(official.questions());
        QuestionDoc q = questions.getFirst();
        questions.add(new QuestionDoc("XXX.1.1", "XXX.1", 3, "LOW", q.text(), q.evidence(),
                q.levels().subList(0, 4), q.recommendations()));

        List<String> errors = validator.validate(new Loaded(official.root(), questions));

        assertThat(errors)
                .anySatisfy(e -> assertThat(e).contains("sous-domaine inconnu XXX.1"))
                .anySatisfy(e -> assertThat(e).contains("5 descriptions de niveaux"));
    }

    @Test
    void detectsDuplicateQuestionCodeAndInvalidEffort() {
        List<QuestionDoc> questions = new ArrayList<>(official.questions());
        QuestionDoc q = questions.getFirst();
        questions.add(new QuestionDoc(q.code(), q.subdomain(), q.criticality(), "ENORME", q.text(),
                q.evidence(), q.levels(), q.recommendations()));

        assertThat(validator.validate(new Loaded(official.root(), questions)))
                .anySatisfy(e -> assertThat(e).contains("code en double"))
                .anySatisfy(e -> assertThat(e).contains("effort invalide"));
    }
}
