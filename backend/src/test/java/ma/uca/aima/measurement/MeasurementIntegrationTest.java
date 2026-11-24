package ma.uca.aima.measurement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import ma.uca.aima.TestcontainersConfiguration;
import ma.uca.aima.support.ApiClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;

/**
 * Import des résultats du labo red-team, sur le scénario de la démonstration de soutenance :
 * l'organisation se déclare bien protégée, l'attaque réussit (score qui baisse),
 * la faille est corrigée et un nouveau passage remplace la mesure (score qui remonte).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MeasurementIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(context);
    }

    @Test
    void redTeamMeasurementsOverrideDeclaredLevelsAndCanBeReplacedAfterAFix() throws Exception {
        String admin = api.adminToken();
        String orgId = api.createOrganization(admin, "Chatbot Lab SA");
        String orgUser = api.createUserAndLogin(admin, "ORG_USER", orgId);
        String redTeam = api.createUserAndLogin(admin, "AUDITOR", null);
        String base = "/api/v1/assessments/" + api.body(api.post(orgUser, "/api/v1/assessments",
                Map.of("organizationId", orgId, "title", "Avant / après red team"))).get("id").asString();

        // L'organisation se déclare bien protégée contre l'injection de prompt
        api.put(orgUser, base + "/answers/LLM.1.1", Map.of("level", 4)).andExpect(status().isOk());
        api.put(orgUser, base + "/answers/LLM.1.2", Map.of("level", 4)).andExpect(status().isOk());
        assertThat(llm1Score(orgUser, base)).isEqualTo(4.0);

        // Une organisation ne peut pas envoyer de mesures sur elle-même
        api.post(orgUser, base + "/measurements", batch("run-0", "LLM.1.1", "PROMPT_INJECTION", 5))
                .andExpect(status().isForbidden());

        // Lot incohérent : refusé en entier, avec toutes les erreurs, et rien n'est enregistré
        api.post(redTeam, base + "/measurements", Map.of("runId", "run-x", "tool", "garak", "results", List.of(
                        item("LLM.1.1", "PROMPT_INJECTION", 1),
                        item("GOV.1.1", "PROMPT_INJECTION", 1),          // mauvaise famille pour cette question
                        item("NOPE.1.1", "OTHER", 1))))                  // question inconnue
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.allOf(
                        Matchers.containsString("GOV.1.1 : la famille PROMPT_INJECTION ne mesure que [LLM.1.1]"),
                        Matchers.containsString("NOPE.1.1 : question inconnue"))));
        api.get(redTeam, base + "/measurements").andExpect(jsonPath("$.length()").value(0));

        // 1. L'attaque réussit : LLM.1.1 mesuré au niveau 1 → écart de 3 niveaux avec le déclaré
        api.post(redTeam, base + "/measurements", batch("run-1", "LLM.1.1", "PROMPT_INJECTION", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recorded").value(1))
                .andExpect(jsonPath("$.measurements[0].declaredLevel").value(4))
                .andExpect(jsonPath("$.measurements[0].measuredLevel").value(1))
                .andExpect(jsonPath("$.measurements[0].gap").value(3));
        // LLM.1 = (3×1 + 2×4) / 5 = 2,2 : la mesure remplace le niveau déclaré dans le calcul
        assertThat(llm1Score(orgUser, base)).isEqualTo(2.2);

        // 2. La faille est corrigée : un nouveau passage remplace la mesure → le score remonte
        api.post(redTeam, base + "/measurements", batch("run-2", "LLM.1.1", "PROMPT_INJECTION", 4))
                .andExpect(status().isOk());
        assertThat(llm1Score(orgUser, base)).isEqualTo(4.0);

        // L'organisation voit ses mesures (sans pouvoir les modifier)
        api.get(orgUser, base + "/measurements")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].runId").value("run-2"))
                .andExpect(jsonPath("$[0].gap").value(0));
        api.delete(orgUser, base + "/measurements/LLM.1.1").andExpect(status().isForbidden());

        // Le contrat famille → questions est publié pour le labo
        api.get(redTeam, "/api/v1/measurements/attack-families")
                .andExpect(jsonPath("$[?(@.code == 'UNSAFE_TOOL_USE')].questionCodes[*]",
                        Matchers.containsInAnyOrder("LLM.3.1", "LLM.3.2")));
    }

    // ------------------------------------------------------------------ utilitaires

    private double llm1Score(String token, String base) throws Exception {
        JsonNode scoring = api.body(api.get(token, base + "/results")).get("scoring");
        for (JsonNode d : scoring.get("domains")) {
            for (JsonNode sd : d.get("subdomains")) {
                if (sd.get("code").asString().equals("LLM.1")) {
                    return sd.get("score").get("value").asDouble();
                }
            }
        }
        throw new AssertionError("LLM.1 introuvable");
    }

    private static Map<String, Object> batch(String runId, String code, String family, int level) {
        return Map.of("runId", runId, "tool", "garak 0.12", "results", List.of(item(code, family, level)));
    }

    private static Map<String, Object> item(String code, String family, int level) {
        return Map.of("questionCode", code, "attackFamily", family, "measuredLevel", level,
                "evidence", "Résultat du passage de tests sur " + code);
    }
}
