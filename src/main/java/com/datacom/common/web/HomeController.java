package com.datacom.common.web;

import com.datacom.product.application.ProductCatalogService;
import com.datacom.user.infrastructure.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProductCatalogService catalogService;

    public HomeController(ProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal UserPrincipal principal) {
        model.addAttribute("firstname", principal.getFirstname());
        model.addAttribute("lastname", principal.getLastname());
        model.addAttribute("role", principal.getRole());
        model.addAttribute(
                "counts", catalogService.homeCounts(principal.getId(), principal.getRole()));
        return "home";
    }
}
