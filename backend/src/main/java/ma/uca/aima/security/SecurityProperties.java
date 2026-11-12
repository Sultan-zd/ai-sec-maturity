package ma.uca.aima.security;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de sécurité ({@code aima.security.*}).
 * En production, le secret JWT et le mot de passe administrateur DOIVENT venir de variables
 * d'environnement (AIMA_JWT_SECRET, AIMA_ADMIN_PASSWORD), jamais du code source.
 */
@ConfigurationProperties(prefix = "aima.security")
public record SecurityProperties(Jwt jwt, BootstrapAdmin bootstrapAdmin, List<String> corsAllowedOrigins) {

    /**
     * @param secret clé HMAC-SHA256 : au moins 32 caractères (256 bits)
     * @param ttl    durée de validité d'un jeton
     */
    public record Jwt(String secret, Duration ttl, String issuer) {
    }

    /** Compte administrateur créé au premier démarrage si aucun utilisateur n'existe. */
    public record BootstrapAdmin(String email, String password, String fullName) {
    }
}
