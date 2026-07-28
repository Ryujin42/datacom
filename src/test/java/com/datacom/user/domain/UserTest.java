package com.datacom.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class UserTest {

    private final Instant now = Instant.parse("2026-07-28T10:00:00Z");

    private User newUser() {
        return new User("operator1", "{bcrypt}hash", "Jean", "Dupont", Role.OPERATOR);
    }

    @Test
    void isNotLockedByDefault() {
        assertThat(newUser().isLocked(now)).isFalse();
    }

    @Test
    void locksAfterFiveFailedAttemptsWithinTheWindow() {
        User user = newUser();
        for (int i = 0; i < 4; i++) {
            user.registerFailedAttempt(now.plusSeconds(i));
        }
        assertThat(user.isLocked(now)).isFalse();

        user.registerFailedAttempt(now.plusSeconds(4));

        assertThat(user.isLocked(now.plusSeconds(4))).isTrue();
        assertThat(user.isLocked(now.plusSeconds(4).plus(15, ChronoUnit.MINUTES).plusSeconds(1)))
                .isFalse();
    }

    @Test
    void resetsTheCounterWhenAttemptsAreMoreThanFifteenMinutesApart() {
        User user = newUser();
        for (int i = 0; i < 4; i++) {
            user.registerFailedAttempt(now.plusSeconds(i));
        }

        // Le 5eme echec arrive 20 minutes plus tard : hors fenetre, le compteur repart de 1.
        Instant fifthAttempt = now.plus(20, ChronoUnit.MINUTES);
        user.registerFailedAttempt(fifthAttempt);

        assertThat(user.isLocked(fifthAttempt)).isFalse();
        assertThat(user.getFailedAttempts()).isEqualTo((short) 1);
    }

    @Test
    void successfulLoginClearsTheCounterAndAnyLock() {
        User user = newUser();
        for (int i = 0; i < 5; i++) {
            user.registerFailedAttempt(now.plusSeconds(i));
        }
        assertThat(user.isLocked(now.plusSeconds(5))).isTrue();

        user.registerSuccessfulLogin();

        assertThat(user.isLocked(now.plusSeconds(5))).isFalse();
        assertThat(user.getFailedAttempts()).isZero();
    }
}
