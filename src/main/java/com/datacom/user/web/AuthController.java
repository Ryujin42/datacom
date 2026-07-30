package com.datacom.user.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AuthController {

    @GetMapping("/login")
    public ModelAndView login(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout) {
        ModelAndView mav = new ModelAndView("auth/login");
        if (error != null) {
            mav.addObject("errorMessage", "Identifiant ou mot de passe incorrect.");
        }
        if (logout != null) {
            mav.addObject("logoutMessage", "Vous avez été déconnecté.");
        }
        return mav;
    }
}
