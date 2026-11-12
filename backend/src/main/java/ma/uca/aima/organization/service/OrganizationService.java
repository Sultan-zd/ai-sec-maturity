package ma.uca.aima.organization.service;

import java.util.List;
import java.util.UUID;
import ma.uca.aima.common.ResourceNotFoundException;
import ma.uca.aima.organization.api.OrganizationDtos.OrganizationDto;
import ma.uca.aima.organization.api.OrganizationDtos.OrganizationRequest;
import ma.uca.aima.organization.model.Organization;
import ma.uca.aima.organization.repository.OrganizationRepository;
import ma.uca.aima.security.AccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository repository;
    private final AccessGuard accessGuard;

    public OrganizationService(OrganizationRepository repository, AccessGuard accessGuard) {
        this.repository = repository;
        this.accessGuard = accessGuard;
    }

    public OrganizationDto create(OrganizationRequest request) {
        Organization organization = new Organization(request.name().trim(), request.sector(), request.size(),
                request.country(), request.description());
        return OrganizationDto.from(repository.saveAndFlush(organization));
    }

    public OrganizationDto update(UUID id, OrganizationRequest request) {
        accessGuard.checkOrganization(id);
        Organization organization = find(id);
        organization.update(request.name().trim(), request.sector(), request.size(), request.country(),
                request.description());
        return OrganizationDto.from(repository.saveAndFlush(organization));
    }

    @Transactional(readOnly = true)
    public List<OrganizationDto> list() {
        UUID scope = accessGuard.organizationScope();
        if (scope != null) {
            return List.of(OrganizationDto.from(find(scope)));      // ORG_USER : sa seule organisation
        }
        return repository.findAllByOrderByNameAsc().stream().map(OrganizationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public OrganizationDto get(UUID id) {
        accessGuard.checkOrganization(id);
        return OrganizationDto.from(find(id));
    }

    /** Recherche interne, sans contrôle d'accès (à utiliser par les autres services). */
    public Organization find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation " + id + " introuvable"));
    }
}
