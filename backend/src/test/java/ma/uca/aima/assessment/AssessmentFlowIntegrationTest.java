package ma.uca.aima.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import ma.uca.aima.TestcontainersConfiguration;
import ma.uca.aima.referential.service.ReferentialQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parcours complet d'une auto-évaluation via l'API REST, sur une vraie base PostgreSQL.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AssessmentFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JsonMapper json;

    @Autowired
    private ReferentialQueryService referentialService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void completeSelfAssessmentFlow() throws Exception {
        // 1. Organisation + évaluation (cible 4 pour le domaine LLM, 3 par défaut ailleurs)
        String orgId = body(send(post("/api/v1/organizations"), Map.of(
                "name", "FinTech Atlas", "sector", "BANKING_FINANCE", "size", "PME", "country", "Maroc"))
                .andExpect(status().isCreated())).get("id").asString();

        JsonNode assessment = body(send(post("/api/v1/assessments"), Map.of(
                "organizationId", orgId, "title", "Évaluation initiale 2026", "targets", Map.of("LLM", 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.targets.GOV").value(3))
                .andExpect(jsonPath("$.targets.LLM").value(4))
                .andExpect(jsonPath("$.totalQuestions").value(59)));
        String base = "/api/v1/assessments/" + assessment.get("id").asString();

        // 2. Erreurs de saisie
        send(put(base + "/answers/GOV.1.1"), Map.of("level", 7))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.level").exists());
        send(put(base + "/answers/GOV.1.1"), Map.of("notApplicable", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La justification est obligatoire pour « Non applicable »"));
        send(put(base + "/answers/NOPE.1.1"), Map.of("level", 3))
                .andExpect(status().isNotFound());
        send(put(base + "/targets"), Map.of("targets", Map.of("XYZ", 3)))
                .andExpect(status().isBadRequest());
        mvc.perform(post(base + "/submit"))
                .andExpect(status().isConflict());

        // 3. Réponses : niveau 3 partout, LLM.3.2 au niveau 1, TRUST.2.2 non applicable
        for (String code : allQuestionCodes()) {
            Map<String, Object> answer = switch (code) {
                case "LLM.3.2" -> Map.of("level", 1, "comment", "Les agents envoient des e-mails sans validation");
                case "TRUST.2.2" -> Map.of("notApplicable", true, "justification", "Pas de système à impact social");
                default -> Map.of("level", 3, "evidence", code.startsWith("GOV") ? "Politique IA v1.pdf" : "");
            };
            send(put(base + "/answers/" + code), answer).andExpect(status().isOk());
        }

        // 4. Résultats
        JsonNode results = body(mvc.perform(get(base + "/results")).andExpect(status().isOk()));
        JsonNode scoring = results.get("scoring");
        assertThat(scoring.get("completionPercent").asDouble()).isEqualTo(100.0);
        assertThat(scoring.get("notApplicable").asInt()).isEqualTo(1);

        JsonNode llm = domain(scoring, "LLM");
        // LLM.3 = (3×3 + 3×1)/6 = 2 ; brut LLM = (3+3+2+3)/4 = 2,75 → plafonné à 2,0 par LLM.3.2 (critique, niveau 1)
        assertThat(llm.get("rawScore").asDouble()).isEqualTo(2.75);
        assertThat(llm.get("score").get("value").asDouble()).isEqualTo(2.0);
        assertThat(llm.get("cappedBy").asString()).isEqualTo("LLM.3.2");
        assertThat(llm.get("gap").asDouble()).isEqualTo(2.0);
        assertThat(domain(scoring, "GOV").get("gap").asDouble()).isEqualTo(0.0);
        // Global = 3 − 0,15 × (3 − 2) = 2,85 → niveau 3 « Défini »
        assertThat(scoring.get("global").get("value").asDouble()).isEqualTo(2.85);
        assertThat(scoring.get("global").get("level").asInt()).isEqualTo(3);
        // Indice de confiance : 9 réponses GOV prouvées sur 58 réponses évaluées
        assertThat(results.get("evidenceRatePercent").asDouble()).isEqualTo(15.52);

        // 5. Recommandations : seules les 8 questions LLM sont sous la cible (4)
        JsonNode recos = scoring.get("recommendations");
        assertThat(recos.size()).isEqualTo(8);
        JsonNode first = recos.get(0);
        assertThat(first.get("questionCode").asString()).isEqualTo("LLM.3.2");
        assertThat(first.get("priority").asDouble()).isEqualTo(135.0);   // (4−1)×3×0,15×100
        assertThat(first.get("horizon").asString()).isEqualTo("QUICK_WIN");
        assertThat(first.get("followUpActions").size()).isEqualTo(2);    // 2→3 puis 3→4

        mvc.perform(get(base + "/roadmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActions").value(8))
                .andExpect(jsonPath("$.phases.length()").value(4))
                .andExpect(jsonPath("$.phases[0].horizon").value("QUICK_WIN"))
                .andExpect(jsonPath("$.phases[0].actions[0].questionCode").value("LLM.3.2"));

        // 6. Soumission : score figé et réponses verrouillées
        mvc.perform(post(base + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.globalScore").value(2.85))
                .andExpect(jsonPath("$.globalLevel").value(3));
        send(put(base + "/answers/GOV.1.1"), Map.of("level", 5))
                .andExpect(status().isConflict());

        // 7. Réouverture
        mvc.perform(post(base + "/reopen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
        send(put(base + "/answers/GOV.1.1"), Map.of("level", 5)).andExpect(status().isOk());
    }

    @Test
    void organizationValidationErrorsAreDetailedPerField() throws Exception {
        send(post("/api/v1/organizations"), Map.of("name", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Données invalides"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.sector").exists())
                .andExpect(jsonPath("$.errors.size").exists());
    }

    // ------------------------------------------------------------------ utilitaires

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               Object payload) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)));
    }

    private JsonNode body(ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private static JsonNode domain(JsonNode scoring, String code) {
        for (JsonNode d : scoring.get("domains")) {
            if (d.get("code").asString().equals(code)) {
                return d;
            }
        }
        throw new AssertionError("Domaine " + code + " absent");
    }

    private List<String> allQuestionCodes() {
        return referentialService.getActive().domains().stream()
                .flatMap(d -> d.subdomains().stream())
                .flatMap(sd -> sd.questions().stream())
                .map(q -> q.code())
                .toList();
    }
}
