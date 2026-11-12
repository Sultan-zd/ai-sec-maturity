package ma.uca.aima.referential.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import ma.uca.aima.referential.api.ReferentialDtos.QuestionDto;
import ma.uca.aima.referential.api.ReferentialDtos.ReferentialDetail;
import ma.uca.aima.referential.api.ReferentialDtos.ReferentialSummary;
import ma.uca.aima.referential.service.ReferentialQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/referentials")
@Tag(name = "Référentiel", description = "Consultation du modèle de maturité : domaines, sous-domaines, questions")
public class ReferentialController {

    private final ReferentialQueryService service;

    public ReferentialController(ReferentialQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lister les versions du référentiel")
    public List<ReferentialSummary> list() {
        return service.listReferentials();
    }

    @GetMapping("/active")
    @Operation(summary = "Référentiel actif complet (utilisé pour les nouvelles évaluations)")
    public ReferentialDetail active() {
        return service.getActive();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Une version du référentiel, complète")
    public ReferentialDetail byId(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/questions/{code}")
    @Operation(summary = "Détail d'une question (niveaux et recommandations)")
    public QuestionDto question(@PathVariable Long id, @PathVariable String code) {
        return service.getQuestion(id, code);
    }
}
