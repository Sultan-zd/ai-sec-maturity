package ma.uca.aima.referential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ma.uca.aima.TestcontainersConfiguration;
import ma.uca.aima.referential.api.ReferentialDtos.QuestionDto;
import ma.uca.aima.referential.api.ReferentialDtos.ReferentialDetail;
import ma.uca.aima.referential.importer.ReferentialImportService;
import ma.uca.aima.referential.importer.ReferentialImportService.Outcome;
import ma.uca.aima.referential.service.ReferentialQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Test d'intégration complet : vrai PostgreSQL (Testcontainers) + migrations Flyway
 * + import du référentiel au démarrage + API REST.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReferentialIntegrationTest {

    @Autowired
    private ReferentialQueryService queryService;

    @Autowired
    private ReferentialImportService importService;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void referentialIsImportedAtStartupWithCompleteTree() {
        ReferentialDetail active = queryService.getActive();

        assertThat(active.code()).isEqualTo("AIMA");
        assertThat(active.active()).isTrue();
        assertThat(active.maturityLevels()).hasSize(5);
        assertThat(active.domains()).hasSize(8);
        assertThat(active.domains().stream().mapToLong(d -> d.subdomains().size()).sum()).isEqualTo(29);
        assertThat(active.domains().stream()
                .flatMap(d -> d.subdomains().stream())
                .mapToLong(sd -> sd.questions().size()).sum()).isEqualTo(59);
        assertThat(active.domains().getFirst().subdomains().getFirst().references())
                .containsKey("ISO42001");
    }

    @Test
    void questionKeepsLevelsAndRecommendations() {
        Long id = queryService.findActive().getId();
        QuestionDto question = queryService.getQuestion(id, "LLM.3.2");

        assertThat(question.criticality()).isEqualTo(3);
        assertThat(question.levels()).hasSize(5);
        assertThat(question.levels().getFirst().recommendation()).isNotBlank();
        assertThat(question.levels().getLast().recommendation()).isNull();
    }

    @Test
    void importIsIdempotent() {
        assertThat(importService.importFromLocation()).isEqualTo(Outcome.ALREADY_PRESENT);
        assertThat(queryService.listReferentials()).hasSize(1);
    }

    @Test
    void apiReturnsActiveReferential() throws Exception {
        mvc.perform(get("/api/v1/referentials/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AIMA"))
                .andExpect(jsonPath("$.domains.length()").value(8))
                .andExpect(jsonPath("$.domains[4].code").value("LLM"));
    }

    @Test
    void apiReturnsProblemDetailWhenQuestionIsUnknown() throws Exception {
        Long id = queryService.findActive().getId();
        mvc.perform(get("/api/v1/referentials/{id}/questions/{code}", id, "NOPE.1.1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Ressource introuvable"));
    }
}
