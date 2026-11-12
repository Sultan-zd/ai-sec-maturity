package ma.uca.aima.support;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Petit client HTTP pour les tests d'intégration : passe par la VRAIE chaîne de sécurité
 * (filtre Spring Security + jetons JWT), exactement comme le ferait le frontend.
 */
public class ApiClient {

    public static final String ADMIN_EMAIL = "admin@aima.local";
    public static final String ADMIN_PASSWORD = "Admin@2026!";
    public static final String USER_PASSWORD = "MotDePasse2026";

    private final MockMvc mvc;
    private final JsonMapper json;

    public ApiClient(WebApplicationContext context) {
        Filter security = context.getBean("springSecurityFilterChain", Filter.class);
        this.mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        this.json = context.getBean(JsonMapper.class);
    }

    // ------------------------------------------------------------------ requêtes (token null = anonyme)

    public ResultActions get(String token, String url) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.get(url), token));
    }

    public ResultActions post(String token, String url, Object body) throws Exception {
        return mvc.perform(withBody(auth(MockMvcRequestBuilders.post(url), token), body));
    }

    public ResultActions put(String token, String url, Object body) throws Exception {
        return mvc.perform(withBody(auth(MockMvcRequestBuilders.put(url), token), body));
    }

    public ResultActions delete(String token, String url) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.delete(url), token));
    }

    public JsonNode body(ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    // ------------------------------------------------------------------ scénarios courants

    public String login(String email, String password) throws Exception {
        ResultActions result = post(null, "/api/v1/auth/login", Map.of("email", email, "password", password))
                .andExpect(status().isOk());
        return body(result).get("accessToken").asString();
    }

    public String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /** Crée une organisation (nom unique) et renvoie son identifiant. */
    public String createOrganization(String adminToken, String name) throws Exception {
        ResultActions result = post(adminToken, "/api/v1/organizations", Map.of(
                "name", name + " " + shortId(), "sector", "BANKING_FINANCE", "size", "PME", "country", "Maroc"))
                .andExpect(status().isCreated());
        return body(result).get("id").asString();
    }

    /** Crée un utilisateur (e-mail unique) et renvoie son jeton de connexion. */
    public String createUserAndLogin(String adminToken, String role, String organizationId) throws Exception {
        String email = role.toLowerCase() + "-" + shortId() + "@test.ma";
        Map<String, Object> request = new HashMap<>(Map.of(
                "email", email, "fullName", "Test " + role, "password", USER_PASSWORD, "role", role));
        if (organizationId != null) {
            request.put("organizationId", organizationId);
        }
        post(adminToken, "/api/v1/users", request).andExpect(status().isCreated());
        return login(email, USER_PASSWORD);
    }

    // ------------------------------------------------------------------ interne

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder request, Object body) {
        return body == null ? request
                : request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
