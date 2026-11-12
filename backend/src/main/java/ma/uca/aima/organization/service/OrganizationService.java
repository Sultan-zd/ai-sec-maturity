package ma.uca.aima.organization.service;

import java.util.List;
import java.util.UUID;
import ma.uca.aima.common.ResourceNotFoundException;
import ma.uca.aima.organization.api.OrganizationDtos.OrganizationDto;
import ma.uca.aima.organization.api.OrganizationDtos.OrganizationRequest;
import ma.uca.aima.organization.model.Organization;
import ma.uca.aima.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository repository;

    public OrganizationService(OrganizationRepository repository) {
        this.repository = repository;
    }

    public OrganizationDto create(OrganizationRequest request) {
        Organization organization = new Organization(request.name().trim(), request.sector(), request.size(),
                request.country(), request.description());
        return OrganizationDto.from(repository.saveAndFlush(organization));
    }

    public OrganizationDto update(UUID id, OrganizationRequest request) {
        Organization organization = find(id);
        organization.update(request.name().trim(), request.sector(), request.size(), request.country(),
                request.description());
        return OrganizationDto.from(repository.saveAndFlush(organization));
    }

    @Transactional(readOnly = true)
    public List<OrganizationDto> list() {
        return repository.findAllByOrderByNameAsc().stream().map(OrganizationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public OrganizationDto get(UUID id) {
        return OrganizationDto.from(find(id));
    }

    public Organization find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation " + id + " introuvable"));
    }
}
