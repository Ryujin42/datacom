package com.datacom.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.datacom.user.domain.Role;
import com.datacom.user.domain.User;
import com.datacom.user.infrastructure.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordChangeServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final SessionRegistry sessionRegistry = mock(SessionRegistry.class);
    private final PasswordChangeService service =
            new PasswordChangeService(
                    userRepository, passwordEncoder, new PasswordPolicy(), sessionRegistry);

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("operator1", "{bcrypt}oldHash", "Jean", "Dupont", Role.OPERATOR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void rejectsAnIncorrectCurrentPassword() {
        when(passwordEncoder.matches("wrong", "{bcrypt}oldHash")).thenReturn(false);

        assertThatThrownBy(
                        () -> service.changePassword(1L, "wrong", "NewPassword1234!", "session-1"))
                .isInstanceOf(InvalidCurrentPasswordException.class);

        verifyNoInteractions(sessionRegistry);
    }

    @Test
    void rejectsANewPasswordThatViolatesThePolicy() {
        when(passwordEncoder.matches("current", "{bcrypt}oldHash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(1L, "current", "short", "session-1"))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }

    @Test
    void changesThePasswordAndInvalidatesOnlyOtherSessions() {
        when(passwordEncoder.matches("current", "{bcrypt}oldHash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1234!")).thenReturn("{bcrypt}newHash");

        SessionInformation currentSession = mock(SessionInformation.class);
        when(currentSession.getSessionId()).thenReturn("session-1");
        SessionInformation otherSession = mock(SessionInformation.class);
        when(otherSession.getSessionId()).thenReturn("session-2");
        when(sessionRegistry.getAllSessions(any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(List.of(currentSession, otherSession));

        service.changePassword(1L, "current", "NewPassword1234!", "session-1");

        assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}newHash");
        verify(currentSession, org.mockito.Mockito.never()).expireNow();
        verify(otherSession).expireNow();
    }
}
