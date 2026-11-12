package ma.uca.aima.assessment.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import ma.uca.aima.assessment.api.AssessmentDtos.AnswerDto;
import ma.uca.aima.assessment.api.AssessmentDtos.AnswerRequest;
import ma.uca.aima.assessment.api.AssessmentDtos.AssessmentResults;
import ma.uca.aima.assessment.api.AssessmentDtos.AssessmentSummary;
import ma.uca.aima.assessment.api.AssessmentDtos.CreateAssessmentRequest;
import ma.uca.aima.assessment.api.AssessmentDtos.Roadmap;
import ma.uca.aima.assessment.api.AssessmentDtos.TargetsRequest;
import ma.uca.aima.assessment.service.AssessmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assessments")
@Tag(name = "Évaluations", description = "Auto-évaluations : réponses, scoring, écarts et feuille de route")
public class AssessmentController {

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Créer une évaluation (avec le référentiel actif)")
    public ResponseEntity<AssessmentSummary> create(@Valid @RequestBody CreateAssessmentRequest request) {
        AssessmentSummary created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/assessments/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Lister les évaluations (filtre optionnel par organisation)")
    public List<AssessmentSummary> list(@RequestParam(required = false) UUID organizationId) {
        return service.list(organizationId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Résumé d'une évaluation (statut, avancement, score figé)")
    public AssessmentSummary get(@PathVariable UUID id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une évaluation en brouillon")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/targets")
    @Operation(summary = "Modifier les niveaux cibles par domaine")
    public AssessmentSummary updateTargets(@PathVariable UUID id, @Valid @RequestBody TargetsRequest request) {
        return service.updateTargets(id, request.targets());
    }

    @GetMapping("/{id}/answers")
    @Operation(summary = "Réponses saisies")
    public List<AnswerDto> answers(@PathVariable UUID id) {
        return service.answers(id);
    }

    @PutMapping("/{id}/answers/{questionCode}")
    @Operation(summary = "Enregistrer (ou modifier) la réponse à une question")
    public AnswerDto saveAnswer(@PathVariable UUID id, @PathVariable String questionCode,
                                @Valid @RequestBody AnswerRequest request) {
        return service.saveAnswer(id, questionCode, request);
    }

    @DeleteMapping("/{id}/answers/{questionCode}")
    @Operation(summary = "Effacer la réponse à une question")
    public ResponseEntity<Void> deleteAnswer(@PathVariable UUID id, @PathVariable String questionCode) {
        service.deleteAnswer(id, questionCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Soumettre l'évaluation (toutes les questions doivent avoir une réponse)")
    public AssessmentSummary submit(@PathVariable UUID id) {
        return service.submit(id);
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Rouvrir une évaluation soumise pour la modifier")
    public AssessmentSummary reopen(@PathVariable UUID id) {
        return service.reopen(id);
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Scores par domaine et sous-domaine, écarts, recommandations")
    public AssessmentResults results(@PathVariable UUID id) {
        return service.results(id);
    }

    @GetMapping("/{id}/roadmap")
    @Operation(summary = "Feuille de route : recommandations regroupées par horizon")
    public Roadmap roadmap(@PathVariable UUID id) {
        return service.roadmap(id);
    }
}
