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
import ma.uca.aima.assessment.api.AssessmentDtos.ReviewRequest;
import ma.uca.aima.assessment.api.AssessmentDtos.Roadmap;
import ma.uca.aima.assessment.api.AssessmentDtos.TargetsRequest;
import ma.uca.aima.assessment.service.AssessmentService;
import ma.uca.aima.report.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Évaluations. Matrice des droits :
 * <pre>
 *                              ADMIN   ORG_USER (sa propre org.)   AUDITOR
 * Consulter / résultats / PDF    ✔           ✔                       ✔
 * Créer, répondre, soumettre     ✔           ✔                       ✘
 * Revoir, valider (audit)        ✔           ✘                       ✔
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/assessments")
@Tag(name = "Évaluations", description = "Auto-évaluations, audit, scoring, écarts, feuille de route et rapport PDF")
public class AssessmentController {

    private static final String RESPONDENT = "hasAnyRole('ADMIN', 'ORG_USER')";
    private static final String AUDITOR = "hasAnyRole('ADMIN', 'AUDITOR')";

    private final AssessmentService service;
    private final ReportService reportService;

    public AssessmentController(AssessmentService service, ReportService reportService) {
        this.service = service;
        this.reportService = reportService;
    }

    // ------------------------------------------------------------------ consultation

    @GetMapping
    @Operation(summary = "Lister les évaluations (un ORG_USER ne voit que celles de son organisation)")
    public List<AssessmentSummary> list(@RequestParam(required = false) UUID organizationId) {
        return service.list(organizationId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Résumé d'une évaluation (statut, avancement, revue, score figé)")
    public AssessmentSummary get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/answers")
    @Operation(summary = "Réponses saisies (avec la revue de l'auditeur)")
    public List<AnswerDto> answers(@PathVariable UUID id) {
        return service.answers(id);
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

    @GetMapping(value = "/{id}/report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Rapport d'évaluation au format PDF")
    public ResponseEntity<byte[]> report(@PathVariable UUID id) {
        ReportService.Report report = reportService.generate(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(report.fileName()).build().toString())
                .body(report.content());
    }

    // ------------------------------------------------------------------ saisie (organisation)

    @PostMapping
    @PreAuthorize(RESPONDENT)
    @Operation(summary = "Créer une évaluation (avec le référentiel actif)")
    public ResponseEntity<AssessmentSummary> create(@Valid @RequestBody CreateAssessmentRequest request) {
        AssessmentSummary created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/assessments/" + created.id())).body(created);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RESPONDENT)
    @Operation(summary = "Supprimer une évaluation en brouillon")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/targets")
    @PreAuthorize(RESPONDENT)
    @Operation(summary = "Modifier les niveaux cibles par domaine")
    public AssessmentSummary updateTargets(@PathVariable UUID id, @Valid @RequestBody TargetsRequest request) {
        return service.updateTargets(id, request.targets());
    }

    @PutMapping("/{id}/answers/{questionCode}")
    @PreAuthorize(RESPONDENT)
    @Operation(summary = "Enregistrer (ou modifier) la réponse à une question")
    public AnswerDto saveAnswer(@PathVariable UUID id, @PathVariable String questionCode,
                                @Valid @RequestBody AnswerRequest request) {
        return service.saveAnswer(id, questionCode, request);
    }

    @DeleteMapping("/{id}/answers/{questionCode}")
    @PreAuthorize(RESPONDENT)
    @Operation(summary = "Effacer la réponse à une question")
    public ResponseEntity<Void> deleteAnswer(@PathVariable UUID id, @PathVariable String questionCode) {
        service.deleteAnswer(id, questionCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize(RESPONDENT)
    @Operation(summary = "Soumettre l'évaluation (toutes les questions doivent avoir une réponse)")
    public AssessmentSummary submit(@PathVariable UUID id) {
        return service.submit(id);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize(RESPONDENT)
    @Operation(summary = "Rouvrir une évaluation soumise (non validée) pour la modifier")
    public AssessmentSummary reopen(@PathVariable UUID id) {
        return service.reopen(id);
    }

    // ------------------------------------------------------------------ audit

    @PutMapping("/{id}/answers/{questionCode}/review")
    @PreAuthorize(AUDITOR)
    @Operation(summary = "Revue d'une réponse par l'auditeur : CONFIRMED, ou ADJUSTED avec le niveau retenu")
    public AnswerDto review(@PathVariable UUID id, @PathVariable String questionCode,
                            @Valid @RequestBody ReviewRequest request) {
        return service.review(id, questionCode, request);
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize(AUDITOR)
    @Operation(summary = "Valider l'audit (toutes les réponses doivent avoir été revues)")
    public AssessmentSummary validate(@PathVariable UUID id) {
        return service.validateAudit(id);
    }
}
