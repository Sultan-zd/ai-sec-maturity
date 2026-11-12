package ma.uca.aima.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import ma.uca.aima.user.model.AppUser;
import ma.uca.aima.user.model.Role;

public final class UserDtos {

    private UserDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserDto user) {
    }

    /** Politique de mot de passe : 10 caractères minimum, avec au moins une lettre et un chiffre. */
    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 255) String fullName,
            @NotBlank @Size(min = 10, max = 100)
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                    message = "doit contenir au moins une lettre et un chiffre") String password,
            @NotNull Role role,
            UUID organizationId) {
    }

    public record UserDto(UUID id, String email, String fullName, Role role, UUID organizationId,
                          String organizationName, boolean enabled, Instant lastLoginAt) {

        public static UserDto from(AppUser u) {
            return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole(),
                    u.getOrganization() == null ? null : u.getOrganization().getId(),
                    u.getOrganization() == null ? null : u.getOrganization().getName(),
                    u.isEnabled(), u.getLastLoginAt());
        }
    }
}
