package com.datacom.product.web;

import com.datacom.product.application.ProductCatalogService;
import com.datacom.product.application.ProductCatalogService.ListOrder;
import com.datacom.product.application.ProductCatalogService.ProductScope;
import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import com.datacom.user.infrastructure.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * US-12 a US-14 : consultation, detail et recherche.
 *
 * <p>La portee (ses fiches / toutes les fiches) est calculee a partir du role porte par la session
 * et transmise au service, qui l'applique. Rien dans l'URL ne permet de l'elargir — c'est ce qui
 * corrige ELEV-4, ou il suffisait de changer un identifiant pour lire la fiche d'un autre.
 */
@Controller
@RequestMapping("/fiches")
public class CatalogController {

    private final ProductCatalogService catalogService;

    public CatalogController(ProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) ProductStatus statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "updatedAt") String tri,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        boolean descending = "updatedAt".equals(tri);
        ListOrder order = ListOrder.of(page, tri, descending);

        model.addAttribute("fiches", catalogService.list(scopeOf(principal), statut, order));
        model.addAttribute("statuts", ProductStatus.values());
        model.addAttribute("statutCourant", statut);
        model.addAttribute("tri", order.sort());
        return "product/list";
    }

    /** US-14, reserve au VALIDATOR : le service refuse tout autre role (SEC-02). */
    @GetMapping("/recherche")
    public String search(
            @RequestParam(defaultValue = "") String terme,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        ListOrder order = ListOrder.of(page, "updatedAt", true);

        model.addAttribute("fiches", catalogService.search(terme, order));
        model.addAttribute("terme", terme);
        return "product/search";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Product product = catalogService.findForReading(id, scopeOf(principal));

        model.addAttribute("fiche", ProductForm.of(product));
        model.addAttribute("historique", catalogService.history(id));
        model.addAttribute("modifiable", principal.getId().equals(product.getCreatedBy()));
        return "product/detail";
    }

    private static ProductScope scopeOf(UserPrincipal principal) {
        return new ProductScope(principal.getId(), principal.getRole());
    }
}
