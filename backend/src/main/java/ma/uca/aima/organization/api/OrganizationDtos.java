package ma.uca.aima.organization.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import ma.uca.aima.organization.model.CompanySize;
import ma.uca.aima.organization.model.Organization;
import ma.uca.aima.organization.model.Sector;

public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    /** Données envoyées pour créer ou modifier une organisation (validées par Bean Validation). */
    public record OrganizationRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull Sector sector,
            @NotNull CompanySize size,
            @Size(max = 100) String country,
            @Size(max = 2000) String description) {
    }

    public record OrganizationDto(UUID id, String name, Sector sector, CompanySize size, String country,
                                  String description, Instant createdAt, Instant updatedAt) {

        public static OrganizationDto from(Organization o) {
            return new OrganizationDto(o.getId(), o.getName(), o.getSector(), o.getSize(), o.getCountry(),
                    o.getDescription(), o.getCreatedAt(), o.getUpdatedAt());
        }
    }
}
