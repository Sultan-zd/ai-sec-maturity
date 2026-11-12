package ma.uca.aima.referential.importer;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/** Importe le référentiel au démarrage de l'application (désactivable par configuration). */
@Component
@ConditionalOnBooleanProperty(name = "aima.referential.import-on-startup", matchIfMissing = true)
public class ReferentialStartupImporter implements ApplicationRunner {

    private final ReferentialImportService importService;

    public ReferentialStartupImporter(ReferentialImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        importService.importFromLocation();
    }
}
