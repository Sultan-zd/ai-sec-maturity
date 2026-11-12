package ma.uca.aima.security;

import java.util.UUID;
import ma.uca.aima.user.model.Role;

/** Utilisateur authentifié, reconstruit à partir des informations du jeton JWT. */
public record CurrentUser(UUID id, String email, Role role, UUID organizationId) {

    public boolean is(Role expected) {
        return role == expected;
    }
}
