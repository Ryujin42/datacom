package com.datacom.common.web;

import com.datacom.user.infrastructure.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Accueil minimal pour L1 : prouve que la protection de session fonctionne de bout en bout. Le
 * veritable ecran (decompte de fiches, actions par role) arrive en US-20 (L5).
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal UserPrincipal principal) {
        model.addAttribute("firstname", principal.getFirstname());
        model.addAttribute("lastname", principal.getLastname());
        model.addAttribute("role", principal.getRole());
        return "home";
    }
}
