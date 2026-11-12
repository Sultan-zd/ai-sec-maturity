package ma.uca.aima.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class LoginAttemptServiceTest {

    /** Horloge manipulable : permet de « faire passer le temps » dans le test. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-12T10:00:00Z");

        void advance(Duration d) { now = now.plus(d); }
        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptService service = new LoginAttemptService(clock);

    @Test
    void blocksAfterFiveFailuresThenUnblocksWhenWindowEnds() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            assertThatCode(() -> service.checkAllowed("Victime@Test.local")).doesNotThrowAnyException();
            service.recordFailure("victime@test.local");
        }
        clock.advance(Duration.ofMinutes(5));

        // E-mail normalisé (casse, espaces) : même compteur
        assertThatThrownBy(() -> service.checkAllowed(" VICTIME@test.local "))
                .isInstanceOfSatisfying(LoginAttemptService.TooManyAttemptsException.class, e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(429);
                    assertThat(e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("600"); // 10 min restantes
                });

        clock.advance(Duration.ofMinutes(10));
        assertThatCode(() -> service.checkAllowed("victime@test.local")).doesNotThrowAnyException();
    }

    @Test
    void successResetsTheCounterAndOtherAccountsAreUnaffected() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) {
            service.recordFailure("a@test.local");
        }
        service.recordSuccess("a@test.local");
        service.recordFailure("a@test.local");
        assertThatCode(() -> service.checkAllowed("a@test.local")).doesNotThrowAnyException();
        assertThatCode(() -> service.checkAllowed("b@test.local")).doesNotThrowAnyException();
    }

    @Test
    void failuresOutsideTheWindowStartANewCount() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) {
            service.recordFailure("c@test.local");
        }
        clock.advance(Duration.ofMinutes(16));
        service.recordFailure("c@test.local");          // nouvelle fenêtre : 1 échec seulement
        assertThatCode(() -> service.checkAllowed("c@test.local")).doesNotThrowAnyException();
    }
}
