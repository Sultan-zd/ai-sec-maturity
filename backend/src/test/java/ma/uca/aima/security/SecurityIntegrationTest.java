package ma.uca.aima.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** Authentification (JWT), autorisations par rôle et cloisonnement entre organisations. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(context);
    }

    // ------------------------------------------------------------------ authentification

    @Test
    void loginReturnsJwtAndCurrentUser() throws Exception {
        JsonNode login = api.body(api.post(null, "/api/v1/auth/login",
                Map.of("email", ApiClient.ADMIN_EMAIL, "password", ApiClient.ADMIN_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("ADMIN")));
        // Un JWT = 3 parties encodées en Base64 séparées par des points
        assertThat(login.get("accessToken").asString().split("\\.")).hasSize(3);

        api.get(login.get("accessToken").asString(), "/api/v1/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ApiClient.ADMIN_EMAIL));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
        String wrongPassword = api.post(null, "/api/v1/auth/login",
                        Map.of("email", ApiClient.ADMIN_EMAIL, "password", "mauvais-mot-de-passe"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = api.post(null, "/api/v1/auth/login",
                        Map.of("email", "inconnu@nulle-part.ma", "password", "mauvais-mot-de-passe"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        // Pas d'énumération possible des comptes : même message dans les deux cas
        assertThat(wrongPassword.replaceAll("\"instance\":\"[^\"]*\"", ""))
                .isEqualTo(unknownEmail.replaceAll("\"instance\":\"[^\"]*\"", ""));
    }

    @Test
    void forgedOrMissingTokenIsRejected() throws Exception {
        api.get(null, "/api/v1/organizations").andExpect(status().isUnauthorized());
        String token = api.adminToken();
        String forged = token.substring(0, token.lastIndexOf('.') + 1) + "signatureFalsifiee";
        api.get(forged, "/api/v1/organizations").andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsStayOpen() throws Exception {
        api.get(null, "/actuator/health").andExpect(status().isOk());
        api.get(null, "/v3/api-docs").andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ rôles

    @Test
    void onlyAdminManagesUsersAndOrganizations() throws Exception {
        String admin = api.adminToken();
        String orgId = api.createOrganization(admin, "Assurance Nord");
        String orgUser = api.createUserAndLogin(admin, "ORG_USER", orgId);
        String auditor = api.createUserAndLogin(admin, "AUDITOR", null);

        api.get(orgUser, "/api/v1/users").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Accès refusé"));
        api.get(auditor, "/api/v1/users").andExpect(status().isForbidden());
        api.post(orgUser, "/api/v1/organizations", Map.of("name", "X", "sector", "OTHER", "size", "TPE"))
                .andExpect(status().isForbidden());
        api.get(admin, "/api/v1/users").andExpect(status().isOk());
    }

    @Test
    void userCreationRules() throws Exception {
        String admin = api.adminToken();
        // Mot de passe trop faible
        api.post(admin, "/api/v1/users", Map.of("email", "faible@test.ma", "fullName", "Faible",
                        "password", "abc", "role", "AUDITOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        // ORG_USER sans organisation
        api.post(admin, "/api/v1/users", Map.of("email", "orphelin@test.ma", "fullName", "Orphelin",
                        "password", "MotDePasse2026", "role", "ORG_USER"))
                .andExpect(status().isBadRequest());
        // E-mail déjà utilisé (insensible à la casse)
        api.post(admin, "/api/v1/users", Map.of("email", "ADMIN@aima.local", "fullName", "Doublon",
                        "password", "MotDePasse2026", "role", "AUDITOR"))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ cloisonnement des organisations

    @Test
    void organizationUsersAreIsolatedFromEachOther() throws Exception {
        String admin = api.adminToken();
        String orgA = api.createOrganization(admin, "Banque A");
        String orgB = api.createOrganization(admin, "Banque B");
        String userA = api.createUserAndLogin(admin, "ORG_USER", orgA);
        String userB = api.createUserAndLogin(admin, "ORG_USER", orgB);
        String auditor = api.createUserAndLogin(admin, "AUDITOR", null);

        String assessmentA = api.body(api.post(userA, "/api/v1/assessments",
                Map.of("organizationId", orgA, "title", "Évaluation A"))).get("id").asString();

        // B ne peut ni lire, ni modifier, ni créer pour A
        api.get(userB, "/api/v1/assessments/" + assessmentA).andExpect(status().isForbidden());
        api.get(userB, "/api/v1/assessments/" + assessmentA + "/results").andExpect(status().isForbidden());
        api.get(userB, "/api/v1/assessments/" + assessmentA + "/report.pdf").andExpect(status().isForbidden());
        api.put(userB, "/api/v1/assessments/" + assessmentA + "/answers/GOV.1.1", Map.of("level", 1))
                .andExpect(status().isForbidden());
        api.post(userB, "/api/v1/assessments", Map.of("organizationId", orgA, "title", "Intrusion"))
                .andExpect(status().isForbidden());
        api.get(userB, "/api/v1/organizations/" + orgA).andExpect(status().isForbidden());

        // Les listes sont filtrées automatiquement
        api.get(userB, "/api/v1/assessments").andExpect(jsonPath("$.length()").value(0));
        api.get(userB, "/api/v1/assessments?organizationId=" + orgA).andExpect(jsonPath("$.length()").value(0));
        api.get(userB, "/api/v1/organizations")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orgB));

        // L'auditeur et l'administrateur ont une vue transverse
        api.get(auditor, "/api/v1/assessments/" + assessmentA).andExpect(status().isOk());
        api.get(admin, "/api/v1/assessments/" + assessmentA).andExpect(status().isOk());
        // … mais l'auditeur ne crée pas d'évaluation
        api.post(auditor, "/api/v1/assessments", Map.of("organizationId", orgA, "title", "Non"))
                .andExpect(status().isForbidden());
    }
}
