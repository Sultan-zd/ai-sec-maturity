package ma.uca.aima.measurement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import ma.uca.aima.measurement.api.MeasurementDtos.AttackFamilyDto;
import ma.uca.aima.measurement.api.MeasurementDtos.MeasurementBatchRequest;
import ma.uca.aima.measurement.api.MeasurementDtos.MeasurementBatchResult;
import ma.uca.aima.measurement.api.MeasurementDtos.MeasurementDto;
import ma.uca.aima.measurement.service.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mesures techniques du labo red-team.
 * <pre>
 *                                   ADMIN   AUDITOR (labo)   ORG_USER (sa propre org.)
 * Envoyer / supprimer des mesures     ✔          ✔                  ✘
 * Consulter les mesures               ✔          ✔                  ✔
 * </pre>
 * Une organisation ne peut pas modifier les mesures qui la concernent : c'est ce qui les rend crédibles.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Mesures red-team", description = "Niveaux mesurés par des tests techniques (déclaré vs mesuré)")
public class MeasurementController {

    private static final String MEASURER = "hasAnyRole('ADMIN', 'AUDITOR')";

    private final MeasurementService service;

    public MeasurementController(MeasurementService service) {
        this.service = service;
    }

    @PostMapping("/assessments/{id}/measurements")
    @PreAuthorize(MEASURER)
    @Operation(summary = "Importer un lot de résultats du labo red-team (tout ou rien)")
    public MeasurementBatchResult ingest(@PathVariable UUID id, @Valid @RequestBody MeasurementBatchRequest request) {
        return service.ingest(id, request);
    }

    @GetMapping("/assessments/{id}/measurements")
    @Operation(summary = "Mesures d'une évaluation, comparées aux niveaux déclarés et audités")
    public List<MeasurementDto> list(@PathVariable UUID id) {
        return service.list(id);
    }

    @DeleteMapping("/assessments/{id}/measurements/{questionCode}")
    @PreAuthorize(MEASURER)
    @Operation(summary = "Supprimer la mesure d'une question")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @PathVariable String questionCode) {
        service.delete(id, questionCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/measurements/attack-families")
    @Operation(summary = "Familles d'attaques acceptées et questions qu'elles mesurent")
    public List<AttackFamilyDto> attackFamilies() {
        return MeasurementService.attackFamilies();
    }
}
