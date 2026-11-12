package ma.uca.aima.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI aimaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AIMA — AI Security Maturity Assessment API")
                .version("v1")
                .description("API d'évaluation de la maturité en cybersécurité et gouvernance de l'IA "
                        + "(ISO/IEC 27001, 42001, 23894, 38507, NIST AI RMF, NIST AI 600-1, OWASP Top 10 LLM)."));
    }
}
