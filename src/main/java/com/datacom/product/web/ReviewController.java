package com.datacom.product.web;

import com.datacom.product.application.ProductReviewService;
import com.datacom.product.application.ProductWorkflowService;
import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductInputException;
import com.datacom.product.domain.UnauthorizedProductActionException;
import com.datacom.user.infrastructure.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/controle")
public class ReviewController {

    private final ProductReviewService reviewService;
    private final ProductWorkflowService workflowService;

    public ReviewController(
            ProductReviewService reviewService, ProductWorkflowService workflowService) {
        this.reviewService = reviewService;
        this.workflowService = workflowService;
    }

    @GetMapping
    public String queue(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("fiches", reviewService.queue(page));
        return "review/queue";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("fiche", ProductForm.of(reviewService.findForReview(id)));
        return "review/detail";
    }

    @PostMapping("/{id}/valider")
    public String validate(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        try {
            workflowService.validate(id, principal.getId());
        } catch (UnauthorizedProductActionException | ProductInputException e) {
            return renderWithError(id, model, e.getMessage());
        }
        return "redirect:/controle";
    }

    @PostMapping("/{id}/renvoyer")
    public String returnToDraft(
            @PathVariable Long id,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        try {
            workflowService.returnToDraft(id, principal.getId(), comment);
        } catch (UnauthorizedProductActionException | ProductInputException e) {
            return renderWithError(id, model, e.getMessage());
        }
        return "redirect:/controle";
    }

    private String renderWithError(Long id, Model model, String message) {
        Product product = reviewService.findForReview(id);
        model.addAttribute("fiche", ProductForm.of(product));
        model.addAttribute("errorMessage", message);
        return "review/detail";
    }
}
