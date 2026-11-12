package ma.uca.aima.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    /** Ajoute le bouton « Authorize » dans Swagger UI pour coller le jeton obtenu via /auth/login. */
    @Bean
    OpenAPI aimaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AIMA — AI Security Maturity Assessment API")
                        .version("v1")
                        .description("API d'évaluation de la maturité en cybersécurité et gouvernance de l'IA "
                                + "(ISO/IEC 27001, 42001, 23894, 38507, NIST AI RMF, NIST AI 600-1, OWASP Top 10 LLM).\n\n"
                                + "1. `POST /api/v1/auth/login` → copier `accessToken`\n"
                                + "2. Bouton **Authorize** → coller le jeton"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
