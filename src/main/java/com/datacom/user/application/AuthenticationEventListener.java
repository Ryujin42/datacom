package com.datacom.user.application;

import com.datacom.user.domain.User;
import com.datacom.user.infrastructure.UserPrincipal;
import com.datacom.user.infrastructure.UserRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class AuthenticationEventListener {

    private final UserRepository userRepository;

    public AuthenticationEventListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener
    @Transactional
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String login = event.getAuthentication().getName();
        log.warn("authentication failed for login {}", login);
        userRepository
                .findByLogin(login)
                .ifPresent(user -> user.registerFailedAttempt(Instant.now()));
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof UserPrincipal principal) {
            log.info("authentication succeeded for login {}", principal.getUsername());
            userRepository
                    .findByLogin(principal.getUsername())
                    .ifPresent(User::registerSuccessfulLogin);
        }
    }
}
