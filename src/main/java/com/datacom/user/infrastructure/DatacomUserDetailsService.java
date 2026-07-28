package com.datacom.user.infrastructure;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatacomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatacomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) {
        return userRepository
                .findByLogin(login)
                .map(UserPrincipal::new)
                // Message generique : ne jamais confirmer qu'un identifiant existe (RG-22, SEC-01).
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "Identifiant ou mot de passe incorrect"));
    }
}
