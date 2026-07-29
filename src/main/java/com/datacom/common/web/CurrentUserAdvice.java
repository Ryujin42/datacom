package com.datacom.common.web;

import com.datacom.user.domain.Role;
import com.datacom.user.infrastructure.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Rend l'identite du utilisateur connecte disponible a toutes les vues, pour que l'en-tete commun
 * puisse l'afficher sans que chaque controleur ait a la reposer dans son modele.
 *
 * <p>Alternative ecartee : la bibliotheque Thymeleaf d'integration Spring Security. Elle ajouterait
 * une dependance et une syntaxe supplementaires pour le seul besoin d'afficher un nom — un attribut
 * de modele suffit et reste lisible par quiconque connait Spring MVC.
 */
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
