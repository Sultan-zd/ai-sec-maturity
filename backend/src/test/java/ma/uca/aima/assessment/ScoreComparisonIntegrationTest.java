package ma.uca.aima.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ma.uca.aima.TestcontainersConfiguration;
import ma.uca.aima.support.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;

/**
 * Fourchette de scores : déclaré / officiel / prouvé. Valeurs calculées à la main.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ScoreComparisonIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(context);
    }

    @Test
    void declaredOfficialAndProvenScoresFormARange() throws Exception {
        String admin = api.adminToken();
        String orgId = api.createOrganization(admin, "Preuves SARL");
        String orgUser = api.createUserAndLogin(admin, "ORG_USER", orgId);
        String redTeam = api.createUserAndLogin(admin, "AUDITOR", null);
        String base = "/api/v1/assessments/" + api.body(api.post(orgUser, "/api/v1/assessments",
                Map.of("organizationId", orgId, "title", "Fourchette"))).get("id").asString();

        // 59 réponses au niveau 3 ; seules les 9 questions GOV sont appuyées par une preuve
        for (String code : questionCodes(admin)) {
            Map<String, Object> answer = code.startsWith("GOV")
                    ? Map.of("level", 3, "evidence", "Politique IA v1.pdf") : Map.of("level", 3);
            api.put(orgUser, base + "/answers/" + code, answer).andExpect(status().isOk());
        }
        // Le labo mesure LLM.1.1 au niveau 1 (injection de prompt réussie)
        api.post(redTeam, base + "/measurements", Map.of("runId", "run-1", "tool", "promptfoo", "results",
                        List.of(Map.of("questionCode", "LLM.1.1", "attackFamily", "PROMPT_INJECTION",
                                "measuredLevel", 1, "evidence", "18/20 injections réussies"))))
                .andExpect(status().isOk());

        JsonNode results = api.body(api.get(orgUser, base + "/results"));
        JsonNode c = results.get("comparison");

        // Déclaré : tout au niveau 3
        assertThat(c.get("declared").get("value").asDouble()).isEqualTo(3.0);
        // Officiel : LLM.1.1 mesuré à 1 (critique) → LLM plafonné à 2,0 ; global = 3 − 0,15 × (3 − 2) = 2,85
        assertThat(c.get("official").get("value").asDouble()).isEqualTo(2.85);
        assertThat(c.get("official").get("value").asDouble())
                .isEqualTo(results.get("scoring").get("global").get("value").asDouble());
        // Prouvé : seules GOV (preuves) et LLM.1.1 (mesurée) comptent ; le reste vaut 1
        // GOV = 3 ; tous les autres domaines = 1 → 0,15 × 3 + 0,85 × 1 = 1,30
        assertThat(c.get("proven").get("value").asDouble()).isEqualTo(1.30);
        assertThat(c.get("provenAnswers").asInt()).isEqualTo(10);        // 9 GOV + LLM.1.1
        assertThat(c.get("evaluatedAnswers").asInt()).isEqualTo(59);

        JsonNode llm = domain(c, "LLM");
        assertThat(llm.get("declared").asDouble()).isEqualTo(3.0);
        assertThat(llm.get("official").asDouble()).isEqualTo(2.0);
        assertThat(llm.get("proven").asDouble()).isEqualTo(1.0);
        assertThat(domain(c, "GOV").get("proven").asDouble()).isEqualTo(3.0);
    }

    private List<String> questionCodes(String token) throws Exception {
        List<String> codes = new ArrayList<>();
        for (JsonNode d : api.body(api.get(token, "/api/v1/referentials/active")).get("domains")) {
            for (JsonNode sd : d.get("subdomains")) {
                for (JsonNode q : sd.get("questions")) {
                    codes.add(q.get("code").asString());
                }
            }
        }
        return codes;
    }

    private static JsonNode domain(JsonNode comparison, String code) {
        for (JsonNode d : comparison.get("domains")) {
            if (d.get("code").asString().equals(code)) {
                return d;
            }
        }
        throw new AssertionError(code + " introuvable");
    }
}
