package ma.uca.aima.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Ressource introuvable → réponse HTTP 404 au format standard « Problem Details » (RFC 9457). */
public class ResourceNotFoundException extends ErrorResponseException {

    public ResourceNotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, problem(detail), null);
    }

    private static ProblemDetail problem(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail);
        problem.setTitle("Ressource introuvable");
        return problem;
    }
}
