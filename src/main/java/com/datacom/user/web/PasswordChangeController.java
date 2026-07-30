package com.datacom.user.web;

import com.datacom.user.application.InvalidCurrentPasswordException;
import com.datacom.user.application.PasswordChangeService;
import com.datacom.user.application.PasswordPolicy;
import com.datacom.user.application.PasswordPolicyViolationException;
import com.datacom.user.infrastructure.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordChangeController {

    private final PasswordChangeService passwordChangeService;

    public PasswordChangeController(PasswordChangeService passwordChangeService) {
        this.passwordChangeService = passwordChangeService;
    }

    @GetMapping("/password/change")
    public String form() {
        return "auth/password-change";
    }

  
    @PostMapping("/password/change")
    public String submit(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            HttpServletRequest request,
            Model model) {
        try {
            passwordChangeService.changePassword(
                    principal.getId(), currentPassword, newPassword, request.getSession().getId());
            model.addAttribute("successMessage", "Mot de passe modifié.");
        } catch (InvalidCurrentPasswordException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (PasswordPolicyViolationException e) {
            model.addAttribute("errorMessage", String.join(" ", e.getViolations()));
        }
        model.addAttribute("minLength", PasswordPolicy.MIN_LENGTH);
        return "auth/password-change";
    }
}
