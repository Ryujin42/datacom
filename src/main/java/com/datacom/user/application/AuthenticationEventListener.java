package com.datacom.user.application;

import com.datacom.user.domain.User;
import com.datacom.user.infrastructure.UserPrincipal;
import com.datacom.user.infrastructure.UserRepository;
import java.time.Instant;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RG-22 : suit les echecs d'authentification par compte, independamment de la couche web. Un
 * identifiant inconnu ne correspond a aucun {@link User} : rien n'est mis a jour, ce qui est
 * correct (et evite un canal lateral revelant l'existence du compte).
 */
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
        userRepository
                .findByLogin(login)
                .ifPresent(user -> user.registerFailedAttempt(Instant.now()));
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof UserPrincipal principal) {
            userRepository
                    .findByLogin(principal.getUsername())
                    .ifPresent(User::registerSuccessfulLogin);
        }
    }
}
