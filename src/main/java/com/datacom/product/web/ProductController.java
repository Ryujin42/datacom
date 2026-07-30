package com.datacom.product.web;

import com.datacom.product.application.ProductEditService;
import com.datacom.product.application.ProductStepData;
import com.datacom.product.application.ProductWorkflowService;
import com.datacom.product.domain.Countries;
import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductInputException;
import com.datacom.user.infrastructure.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/fiches")
public class ProductController {

    private final ProductEditService editService;
    private final ProductWorkflowService workflowService;

    public ProductController(
            ProductEditService editService, ProductWorkflowService workflowService) {
        this.editService = editService;
        this.workflowService = workflowService;
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserPrincipal principal) {
        Long id = editService.create(principal.getId());
        return "redirect:/fiches/" + id + "/etape/1";
    }

    @GetMapping("/{id}/etape/{step}")
    public String showStep(
            @PathVariable Long id,
            @PathVariable int step,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        Product product = editService.findForAuthor(id, principal.getId());
        populate(model, ProductForm.of(product), step);
        return "product/form";
    }

    @PostMapping("/{id}/etape/{step}")
    public String saveStep(
            @PathVariable Long id,
            @PathVariable int step,
            @ModelAttribute StepSubmission submission,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        Long userId = principal.getId();
        try {
            persist(id, userId, step, submission);
        } catch (ProductInputException e) {
            return renderWithError(id, userId, step, submission, model, e);
        }
        int target = submission.cible() == null ? step : submission.cible();
        editService.moveToStep(id, userId, target);
        return "redirect:/fiches/" + id + "/etape/" + target;
    }

    @PostMapping("/{id}/soumettre")
    public String submit(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Long userId = principal.getId();
        try {
            workflowService.submit(id, userId);
        } catch (ProductInputException e) {
            Product product = editService.findForAuthor(id, userId);
            populate(model, ProductForm.of(product), product.getCurrentStep());
            model.addAttribute("errorMessage", e.getMessage());
            return "product/form";
        }
        return "redirect:/fiches/" + id + "/etape/4";
    }

    private void persist(Long id, Long userId, int step, StepSubmission form) {
        long version = form.version();
        switch (step) {
            case 1 ->
                    editService.saveIdentification(
                            id,
                            userId,
                            version,
                            new ProductStepData.Identification(
                                    form.reference(), form.name(), form.description()));
            case 2 ->
                    editService.saveClassification(
                            id,
                            userId,
                            version,
                            new ProductStepData.Classification(
                                    form.category(),
                                    form.subcategory(),
                                    form.manufacturer(),
                                    form.country()));
            case 3 ->
                    editService.saveTraceability(
                            id,
                            userId,
                            version,
                            new ProductStepData.Traceability(
                                    form.lotNumber(), form.certification(), form.authorComment()));
            case 4 -> {}
            default -> throw new IllegalArgumentException("Etape invalide : " + step);
        }
    }

    private String renderWithError(
            Long id,
            Long userId,
            int step,
            StepSubmission submission,
            Model model,
            ProductInputException error) {
        Product product = editService.findForAuthor(id, userId);
        populate(model, SubmittedValues.merge(ProductForm.of(product), submission, step), step);
        model.addAttribute("errorMessage", error.getMessage());
        return "product/form";
    }

    private void populate(Model model, ProductForm form, int step) {
        model.addAttribute("fiche", form);
        model.addAttribute("step", step);
        model.addAttribute("countries", Countries.all());
        model.addAttribute("returnComment", editService.lastReturnComment(form.id()).orElse(null));
    }
}
