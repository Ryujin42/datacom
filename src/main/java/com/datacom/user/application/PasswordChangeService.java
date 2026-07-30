package com.datacom.user.application;

import com.datacom.user.domain.User;
import com.datacom.user.infrastructure.UserPrincipal;
import com.datacom.user.infrastructure.UserRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final SessionRegistry sessionRegistry;

    public PasswordChangeService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional
    public void changePassword(
            Long userId, String currentPassword, String newPassword, String currentSessionId) {
        User user = userRepository.findById(userId).orElseThrow();

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        List<String> violations = passwordPolicy.validate(newPassword);
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        invalidateOtherSessions(new UserPrincipal(user), currentSessionId);
        log.info("password changed for user {}, other sessions invalidated", userId);
    }

    private void invalidateOtherSessions(UserPrincipal principal, String currentSessionId) {
        for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
            if (!session.getSessionId().equals(currentSessionId)) {
                session.expireNow();
            }
        }
    }
}
