package ma.uca.aima.referential.importer;

import java.util.List;

/** Levée lorsque le référentiel JSON est incohérent : l'application refuse alors de démarrer. */
public class InvalidReferentialException extends RuntimeException {

    private final List<String> errors;

    public InvalidReferentialException(List<String> errors) {
        super("Référentiel invalide (" + errors.size() + " erreur(s)) :\n - " + String.join("\n - ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
