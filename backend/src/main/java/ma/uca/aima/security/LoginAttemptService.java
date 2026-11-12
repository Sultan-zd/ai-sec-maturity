package ma.uca.aima.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;

/**
 * Protection contre la force brute sur la connexion (OWASP A07 / API4).
 * <p>
 * Après {@value #MAX_FAILURES} échecs pour un même e-mail dans une fenêtre de 15 minutes, les tentatives
 * suivantes sont refusées (HTTP 429) jusqu'à la fin de la fenêtre. Une connexion réussie remet le compteur à zéro.
 * <p>
 * Compromis assumé : le blocage est temporaire (15 min) pour qu'un attaquant ne puisse pas bloquer un compte
 * durablement. Le stockage est en mémoire : suffisant pour une instance ; avec plusieurs instances, il faudrait
 * un stockage partagé (Redis, par exemple).
 */
@Component
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Attempts(int failures, Instant windowStart) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    /** Lève une erreur 429 si l'e-mail a dépassé le nombre d'échecs autorisés. */
    public void checkAllowed(String email) {
        Attempts current = attempts.get(key(email));
        if (current == null || current.failures() < MAX_FAILURES) {
            return;
        }
        Duration remaining = Duration.between(clock.instant(), current.windowStart().plus(WINDOW));
        if (remaining.isNegative() || remaining.isZero()) {
            attempts.remove(key(email));
            return;
        }
        throw new TooManyAttemptsException(remaining);
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        attempts.merge(key(email), new Attempts(1, now), (old, ignored) ->
                expired(old, now) ? new Attempts(1, now) : new Attempts(old.failures() + 1, old.windowStart()));
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.values().removeIf(a -> expired(a, now));     // évite une croissance infinie de la mémoire
        }
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private static boolean expired(Attempts a, Instant now) {
        return !now.isBefore(a.windowStart().plus(WINDOW));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Réponse 429 au format Problem Details, avec l'en-tête Retry-After (en secondes). */
    static final class TooManyAttemptsException extends ErrorResponseException {

        private final long retryAfterSeconds;

        TooManyAttemptsException(Duration remaining) {
            super(HttpStatus.TOO_MANY_REQUESTS, problem(), null);
            this.retryAfterSeconds = Math.max(1, (remaining.toMillis() + 999) / 1000);
        }

        private static ProblemDetail problem() {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives de connexion échouées. Réessayez plus tard.");
            problem.setTitle("Trop de tentatives");
            return problem;
        }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
            return headers;
        }
    }
}
