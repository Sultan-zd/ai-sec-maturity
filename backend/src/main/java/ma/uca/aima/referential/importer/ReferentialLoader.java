package ma.uca.aima.referential.importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import ma.uca.aima.referential.importer.ReferentialDocument.Loaded;
import ma.uca.aima.referential.importer.ReferentialDocument.QuestionDoc;
import ma.uca.aima.referential.importer.ReferentialDocument.QuestionFile;
import ma.uca.aima.referential.importer.ReferentialDocument.Root;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lit le référentiel depuis {@code aima.referential.location} :
 * un fichier {@code referentiel-*.json} et les fichiers {@code questions/*.json}.
 */
@Component
public class ReferentialLoader {

    private final ResourcePatternResolver resolver;
    private final String location;
    // Lecture stricte : une propriété inconnue dans le JSON (faute de frappe…) est une erreur.
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public ReferentialLoader(ResourcePatternResolver resolver,
                             @Value("${aima.referential.location}") String location) {
        this.resolver = resolver;
        this.location = location;
    }

    public Loaded load() {
        Resource[] roots = find(location + "/referentiel-*.json");
        if (roots.length != 1) {
            throw new InvalidReferentialException(List.of(
                    "Exactement un fichier referentiel-*.json attendu dans " + location + ", trouvé : " + roots.length));
        }
        Root root = read(roots[0], Root.class);

        Resource[] questionFiles = find(location + "/questions/*.json");
        Arrays.sort(questionFiles, Comparator.comparing(Resource::getFilename));
        List<QuestionDoc> questions = new ArrayList<>();
        for (Resource file : questionFiles) {
            QuestionFile content = read(file, QuestionFile.class);
            if (content.questions() != null) {
                questions.addAll(content.questions());
            }
        }
        return new Loaded(root, questions);
    }

    private Resource[] find(String pattern) {
        try {
            return resolver.getResources(pattern);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lister " + pattern, e);
        }
    }

    private <T> T read(Resource resource, Class<T> type) {
        try (InputStream in = resource.getInputStream()) {
            return jsonMapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire " + resource.getFilename(), e);
        } catch (RuntimeException e) {
            throw new InvalidReferentialException(List.of(
                    "JSON invalide dans " + resource.getFilename() + " : " + e.getMessage()));
        }
    }
}
