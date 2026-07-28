package com.datacom.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.datacom.user.domain.Role;
import com.datacom.user.domain.User;
import com.datacom.user.infrastructure.UserPrincipal;
import com.datacom.user.infrastructure.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

class AuthenticationEventListenerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthenticationEventListener listener =
            new AuthenticationEventListener(userRepository);

    @Test
    void registersAFailedAttemptOnTheMatchingUser() {
        User user = new User("operator1", "{bcrypt}hash", "Jean", "Dupont", Role.OPERATOR);
        when(userRepository.findByLogin("operator1")).thenReturn(Optional.of(user));

        listener.onFailure(failureEventFor("operator1"));

        assertThat(user.isLocked(Instant.now().plusSeconds(1))).isFalse();
        assertThat(user.getFailedAttempts()).isEqualTo((short) 1);
    }

    @Test
    void doesNothingWhenTheLoginDoesNotMatchAnyAccount() {
        when(userRepository.findByLogin("admin' --")).thenReturn(Optional.empty());

        // Ne doit pas lever d'exception : un identifiant inconnu n'a aucun compte a mettre a jour.
        listener.onFailure(failureEventFor("admin' --"));
    }

    @Test
    void resetsTheCounterOnSuccessfulLogin() {
        User user = new User("operator1", "{bcrypt}hash", "Jean", "Dupont", Role.OPERATOR);
        user.registerFailedAttempt(Instant.now());
        when(userRepository.findByLogin("operator1")).thenReturn(Optional.of(user));

        listener.onSuccess(
                new AuthenticationSuccessEvent(
                        new TestingAuthenticationToken(new UserPrincipal(user), null)));

        assertThat(user.getFailedAttempts()).isZero();
    }

    private AbstractAuthenticationFailureEvent failureEventFor(String login) {
        return new org.springframework.security.authentication.event
                .AuthenticationFailureBadCredentialsEvent(
                new TestingAuthenticationToken(login, "irrelevant"),
                new BadCredentialsException("bad credentials"));
    }
}
