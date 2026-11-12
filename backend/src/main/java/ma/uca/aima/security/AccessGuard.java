package ma.uca.aima.security;

import java.util.UUID;
import ma.uca.aima.user.model.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Contrôle d'accès « au niveau des données » : un utilisateur ORG_USER ne peut voir et modifier
 * que les données de sa propre organisation. ADMIN et AUDITOR ont une vue transverse.
 * (Les rôles eux-mêmes sont vérifiés dans les contrôleurs avec @PreAuthorize.)
 */
@Component
public class AccessGuard {

    public CurrentUser current() {
        return TokenService.currentUser();
    }

    public void checkOrganization(UUID organizationId) {
        CurrentUser user = current();
        if (user.is(Role.ORG_USER) && !organizationId.equals(user.organizationId())) {
            // Message volontairement neutre : on ne confirme pas l'existence de la ressource.
            throw new AccessDeniedException("Accès refusé à cette organisation");
        }
    }

    /** Organisation imposée pour un ORG_USER, ou {@code null} (pas de filtre) pour ADMIN / AUDITOR. */
    public UUID organizationScope() {
        CurrentUser user = current();
        return user.is(Role.ORG_USER) ? user.organizationId() : null;
    }
}
