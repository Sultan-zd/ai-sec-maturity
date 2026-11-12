package ma.uca.aima.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import ma.uca.aima.TestcontainersConfiguration;
import ma.uca.aima.referential.service.ReferentialQueryService;
import ma.uca.aima.support.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;

/**
 * Parcours complet via l'API REST sécurisée, sur une vraie base PostgreSQL :
 * auto-évaluation par l'organisation, puis audit et validation par un auditeur.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AssessmentFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ReferentialQueryService referentialService;

    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(context);
    }

    @Test
    void selfAssessmentThenAuditFlow() throws Exception {
        // 0. L'administrateur crée l'organisation, un utilisateur de l'organisation et un auditeur
        String admin = api.adminToken();
        String orgId = api.createOrganization(admin, "FinTech Atlas");
        String orgUser = api.createUserAndLogin(admin, "ORG_USER", orgId);
        String auditor = api.createUserAndLogin(admin, "AUDITOR", null);

        // 1. L'organisation crée son évaluation (cible 4 pour LLM, 3 ailleurs)
        JsonNode assessment = api.body(api.post(orgUser, "/api/v1/assessments", Map.of(
                "organizationId", orgId, "title", "Évaluation initiale 2026", "targets", Map.of("LLM", 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.targets.GOV").value(3))
                .andExpect(jsonPath("$.targets.LLM").value(4))
                .andExpect(jsonPath("$.totalQuestions").value(59)));
        String base = "/api/v1/assessments/" + assessment.get("id").asString();

        // 2. Erreurs de saisie
        api.put(orgUser, base + "/answers/GOV.1.1", Map.of("level", 7))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.level").exists());
        api.put(orgUser, base + "/answers/GOV.1.1", Map.of("notApplicable", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La justification est obligatoire pour « Non applicable »"));
        api.put(orgUser, base + "/answers/NOPE.1.1", Map.of("level", 3))
                .andExpect(status().isNotFound());
        api.put(orgUser, base + "/targets", Map.of("targets", Map.of("XYZ", 3)))
                .andExpect(status().isBadRequest());
        api.post(orgUser, base + "/submit", null)
                .andExpect(status().isConflict());

        // 3. Réponses : niveau 3 partout, LLM.3.2 au niveau 1, TRUST.2.2 non applicable
        for (String code : allQuestionCodes()) {
            Map<String, Object> answer = switch (code) {
                case "LLM.3.2" -> Map.of("level", 1, "comment", "Les agents envoient des e-mails sans validation");
                case "TRUST.2.2" -> Map.of("notApplicable", true, "justification", "Pas de système à impact social");
                default -> Map.of("level", 3, "evidence", code.startsWith("GOV") ? "Politique IA v1.pdf" : "");
            };
            api.put(orgUser, base + "/answers/" + code, answer).andExpect(status().isOk());
        }

        // 4. Résultats déclaratifs
        JsonNode results = api.body(api.get(orgUser, base + "/results").andExpect(status().isOk()));
        JsonNode scoring = results.get("scoring");
        assertThat(scoring.get("completionPercent").asDouble()).isEqualTo(100.0);
        JsonNode llm = domain(scoring, "LLM");
        // LLM.3 = (3×3 + 3×1)/6 = 2 ; brut LLM = (3+3+2+3)/4 = 2,75 → plafonné à 2,0 par LLM.3.2
        assertThat(llm.get("rawScore").asDouble()).isEqualTo(2.75);
        assertThat(llm.get("score").get("value").asDouble()).isEqualTo(2.0);
        assertThat(llm.get("cappedBy").asString()).isEqualTo("LLM.3.2");
        // Global = 3 − 0,15 × (3 − 2) = 2,85
        assertThat(scoring.get("global").get("value").asDouble()).isEqualTo(2.85);
        // Indice de confiance : 9 réponses GOV prouvées sur 58 évaluées
        assertThat(results.get("evidenceRatePercent").asDouble()).isEqualTo(15.52);

        JsonNode recos = scoring.get("recommendations");
        assertThat(recos.size()).isEqualTo(8);                          // les 8 questions LLM sous la cible 4
        assertThat(recos.get(0).get("questionCode").asString()).isEqualTo("LLM.3.2");
        assertThat(recos.get(0).get("priority").asDouble()).isEqualTo(135.0);
        assertThat(recos.get(0).get("horizon").asString()).isEqualTo("QUICK_WIN");

        api.get(orgUser, base + "/roadmap")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActions").value(8))
                .andExpect(jsonPath("$.phases[0].actions[0].questionCode").value("LLM.3.2"));

        // 5. Soumission : score figé, réponses verrouillées
        api.post(orgUser, base + "/submit", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.globalScore").value(2.85));
        api.put(orgUser, base + "/answers/GOV.1.1", Map.of("level", 5))
                .andExpect(status().isConflict());

        // 6. Audit : l'organisation ne peut pas s'auto-auditer, l'auditeur ne peut pas répondre
        api.put(orgUser, base + "/answers/GOV.1.1/review", Map.of("decision", "CONFIRMED"))
                .andExpect(status().isForbidden());
        api.put(auditor, base + "/answers/GOV.1.1", Map.of("level", 5))
                .andExpect(status().isForbidden());
        api.put(auditor, base + "/answers/LLM.3.2/review", Map.of("decision", "ADJUSTED", "auditedLevel", 3))
                .andExpect(status().isBadRequest());                     // commentaire obligatoire
        api.post(auditor, base + "/validate", null)
                .andExpect(status().isConflict());                       // rien n'est encore revu

        // L'auditeur constate qu'une validation humaine existe en fait : LLM.3.2 corrigé à 3
        for (String code : allQuestionCodes()) {
            Map<String, Object> review = code.equals("LLM.3.2")
                    ? Map.of("decision", "ADJUSTED", "auditedLevel", 3,
                            "auditorComment", "Workflow d'approbation constaté dans l'outil de ticketing")
                    : Map.of("decision", "CONFIRMED");
            api.put(auditor, base + "/answers/" + code + "/review", review).andExpect(status().isOk());
        }
        api.get(auditor, base + "/answers")
                .andExpect(jsonPath("$[?(@.questionCode == 'LLM.3.2')].effectiveLevel").value(3))
                .andExpect(jsonPath("$[?(@.questionCode == 'LLM.3.2')].level").value(1));

        // 7. Validation : plus de plafond, LLM = 3, global audité = 3,00
        api.post(auditor, base + "/validate", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.reviewedCount").value(59))
                .andExpect(jsonPath("$.globalScore").value(3.0))
                .andExpect(jsonPath("$.globalLevel").value(3));
        JsonNode audited = api.body(api.get(orgUser, base + "/results")).get("scoring");
        assertThat(domain(audited, "LLM").get("cappedBy").isNull()).isTrue();

        // 8. Une évaluation validée est définitive
        api.post(orgUser, base + "/reopen", null).andExpect(status().isConflict());
        api.put(orgUser, base + "/answers/GOV.1.1", Map.of("level", 5)).andExpect(status().isConflict());
    }

    @Test
    void reopeningCancelsPartialReviews() throws Exception {
        String admin = api.adminToken();
        String orgId = api.createOrganization(admin, "Retail Sud");
        String orgUser = api.createUserAndLogin(admin, "ORG_USER", orgId);
        String auditor = api.createUserAndLogin(admin, "AUDITOR", null);
        String base = "/api/v1/assessments/" + api.body(api.post(orgUser, "/api/v1/assessments",
                Map.of("organizationId", orgId, "title", "Test réouverture"))).get("id").asString();
        for (String code : allQuestionCodes()) {
            api.put(orgUser, base + "/answers/" + code, Map.of("level", 2));
        }
        api.post(orgUser, base + "/submit", null).andExpect(status().isOk());
        api.put(auditor, base + "/answers/GOV.1.1/review", Map.of("decision", "CONFIRMED"))
                .andExpect(status().isOk());

        api.post(orgUser, base + "/reopen", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.reviewedCount").value(0));
    }

    @Test
    void organizationValidationErrorsAreDetailedPerField() throws Exception {
        api.post(api.adminToken(), "/api/v1/organizations", Map.of("name", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Données invalides"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.sector").exists())
                .andExpect(jsonPath("$.errors.size").exists());
    }

    // ------------------------------------------------------------------ utilitaires

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
