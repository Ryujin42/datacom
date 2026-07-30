package com.datacom.common.web;

import com.datacom.user.domain.Role;
import com.datacom.user.infrastructure.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("currentUser")
    public String currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        return "%s %s — %s"
                .formatted(principal.getFirstname(), principal.getLastname(), label(principal));
    }

    private static String label(UserPrincipal principal) {
        return principal.getRole() == Role.VALIDATOR
                ? "Responsable conformité"
                : "Opérateur de saisie";
    }
}
