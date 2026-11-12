package ma.uca.aima.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Violation d'une règle métier.
 * <ul>
 *   <li>{@link #invalid(String)} → 400 : la demande elle-même est incorrecte (ex. niveau 7).</li>
 *   <li>{@link #conflict(String)} → 409 : la demande est correcte mais impossible dans l'état actuel
 *       (ex. modifier une évaluation déjà soumise).</li>
 * </ul>
 */
public class BusinessRuleException extends ErrorResponseException {

    private BusinessRuleException(HttpStatus status, String title, String detail) {
        super(status, problem(status, title, detail), null);
    }

    public static BusinessRuleException invalid(String detail) {
        return new BusinessRuleException(HttpStatus.BAD_REQUEST, "Requête invalide", detail);
    }

    public static BusinessRuleException conflict(String detail) {
        return new BusinessRuleException(HttpStatus.CONFLICT, "Opération impossible dans l'état actuel", detail);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
