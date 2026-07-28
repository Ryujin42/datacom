package com.datacom.user.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * US-01/02/03. Les actions POST /login et /logout sont gerees entierement par la chaine de filtres
 * Spring Security (SecurityConfig) - ce controleur ne fait que servir la page HTML.
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public ModelAndView login(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout) {
        ModelAndView mav = new ModelAndView("auth/login");
        if (error != null) {
            // Message generique, identique quelle que soit la cause reelle (RG-22/CA-2/CA-6).
            mav.addObject("errorMessage", "Identifiant ou mot de passe incorrect.");
        }
        if (logout != null) {
            mav.addObject("logoutMessage", "Vous avez été déconnecté.");
        }
        return mav;
    }
}
