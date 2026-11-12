package ma.uca.aima.organization.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import ma.uca.aima.organization.api.OrganizationDtos.OrganizationDto;
import ma.uca.aima.organization.api.OrganizationDtos.OrganizationRequest;
import ma.uca.aima.organization.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organisations", description = "Organisations évaluées")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une organisation (ADMIN)")
    public ResponseEntity<OrganizationDto> create(@Valid @RequestBody OrganizationRequest request) {
        OrganizationDto created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/organizations/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Lister les organisations")
    public List<OrganizationDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une organisation")
    public OrganizationDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG_USER')")
    @Operation(summary = "Modifier une organisation (ADMIN, ou membre de l'organisation)")
    public OrganizationDto update(@PathVariable UUID id, @Valid @RequestBody OrganizationRequest request) {
        return service.update(id, request);
    }
}
