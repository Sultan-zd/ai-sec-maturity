package ma.uca.aima.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import ma.uca.aima.user.model.AppUser;
import ma.uca.aima.user.model.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

/** Émission des jetons JWT et lecture de l'utilisateur courant. */
@Service
public class TokenService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ORG = "org";

    private final JwtEncoder encoder;
    private final SecurityProperties properties;

    public TokenService(JwtEncoder encoder, SecurityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    public IssuedToken issue(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.jwt().ttl());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_NAME, user.getFullName())
                .claim(SecurityConfig.ROLES_CLAIM, List.of(user.getRole().name()));
        if (user.getOrganization() != null) {
            claims.claim(CLAIM_ORG, user.getOrganization().getId().toString());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    /** Utilisateur de la requête en cours (l'API est entièrement authentifiée, sauf /auth/login). */
    public static CurrentUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Aucun utilisateur authentifié");
        }
        List<String> roles = jwt.getClaimAsStringList(SecurityConfig.ROLES_CLAIM);
        String org = jwt.getClaimAsString(CLAIM_ORG);
        return new CurrentUser(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString(CLAIM_EMAIL),
                Role.valueOf(roles.getFirst()), org == null ? null : UUID.fromString(org));
    }
}
