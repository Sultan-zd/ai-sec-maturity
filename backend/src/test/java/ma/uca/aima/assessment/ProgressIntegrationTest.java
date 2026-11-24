package ma.uca.aima.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

/** Progression entre deux évaluations d'une même organisation. Valeurs calculées à la main. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProgressIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(context);
    }

    @Test
    void comparesTwoAssessmentsOfTheSameOrganization() throws Exception {
        String admin = api.adminToken();
        String orgId = api.createOrganization(admin, "Progrès SA");
        String orgUser = api.createUserAndLogin(admin, "ORG_USER", orgId);
        List<String> codes = questionCodes(admin);

        // Janvier : tout au niveau 2 → global 2,00 ; 59 questions sous la cible 3
        String january = create(orgUser, orgId, "Janvier");
        for (String code : codes) {
            api.put(orgUser, "/api/v1/assessments/" + january + "/answers/" + code, Map.of("level", 2));
        }
        // Juin : tout au niveau 3, sauf LLM.3.2 qui régresse à 1
        // LLM.3 = (3×3 + 3×1)/6 = 2 ; LLM brut = 2,75 → plafonné à 2,0 ; global = 3 − 0,15 × 1 = 2,85
        String june = create(orgUser, orgId, "Juin");
        for (String code : codes) {
            api.put(orgUser, "/api/v1/assessments/" + june + "/answers/" + code,
                    Map.of("level", code.equals("LLM.3.2") ? 1 : 3));
        }

        JsonNode p = api.body(api.get(orgUser, "/api/v1/assessments/compare?from=" + january + "&to=" + june)
                .andExpect(status().isOk()));
        assertThat(p.get("sameReferential").asBoolean()).isTrue();
        assertThat(p.get("fromGlobal").get("value").asDouble()).isEqualTo(2.0);
        assertThat(p.get("toGlobal").get("value").asDouble()).isEqualTo(2.85);
        assertThat(p.get("globalDelta").asDouble()).isEqualTo(0.85);
        assertThat(domain(p, "GOV").get("delta").asDouble()).isEqualTo(1.0);
        assertThat(domain(p, "LLM").get("delta").asDouble()).isEqualTo(0.0);   // progrès annulé par le maillon faible
        assertThat(p.get("improved").size()).isEqualTo(58);
        assertThat(p.get("regressed").size()).isEqualTo(1);
        assertThat(p.get("regressed").get(0).get("questionCode").asString()).isEqualTo("LLM.3.2");
        assertThat(p.get("fromRecommendations").asInt()).isEqualTo(59);
        assertThat(p.get("toRecommendations").asInt()).isEqualTo(1);
        assertThat(p.get("closedRecommendations").size()).isEqualTo(58);

        // Une évaluation partielle (1 réponse) ne doit pas « clôturer » les questions laissées sans réponse
        String partial = create(orgUser, orgId, "Partielle");
        api.put(orgUser, "/api/v1/assessments/" + partial + "/answers/GOV.1.1", Map.of("level", 3));
        JsonNode partialProgress = api.body(api.get(orgUser,
                "/api/v1/assessments/compare?from=" + january + "&to=" + partial));
        assertThat(partialProgress.get("closedRecommendations").size()).isEqualTo(1);       // GOV.1.1 seulement
        assertThat(partialProgress.get("toReliable").asBoolean()).isFalse();

        // Refus : même évaluation, organisations différentes, organisation étrangère
        api.get(orgUser, "/api/v1/assessments/compare?from=" + june + "&to=" + june)
                .andExpect(status().isBadRequest());
        String otherOrg = api.createOrganization(admin, "Autre SA");
        String otherUser = api.createUserAndLogin(admin, "ORG_USER", otherOrg);
        String other = create(otherUser, otherOrg, "Autre");
        api.get(admin, "/api/v1/assessments/compare?from=" + january + "&to=" + other)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Les deux évaluations doivent concerner la même organisation"));
        api.get(otherUser, "/api/v1/assessments/compare?from=" + january + "&to=" + june)
                .andExpect(status().isForbidden());
    }

    private String create(String token, String orgId, String title) throws Exception {
        return api.body(api.post(token, "/api/v1/assessments", Map.of("organizationId", orgId, "title", title)))
                .get("id").asString();
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

    private static JsonNode domain(JsonNode progress, String code) {
        for (JsonNode d : progress.get("domains")) {
            if (d.get("code").asString().equals(code)) {
                return d;
            }
        }
        throw new AssertionError(code + " introuvable");
    }
}
