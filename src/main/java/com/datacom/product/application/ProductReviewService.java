package com.datacom.product.application;

import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import com.datacom.product.infrastructure.ProductRepository;
import com.datacom.product.infrastructure.ReviewQueueItem;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-09/US-10 : ce que le responsable conformite consulte avant de decider.
 *
 * <p>SEC-02 : la classe entiere est reservee au role VALIDATOR — un OPERATOR n'atteint aucune de
 * ces methodes, meme en forgeant l'URL (US-09 CA-4, US-10 CA-4, corrige CRIT-3). Les decisions
 * elles-memes vivent dans {@link ProductWorkflowService}, qui porte deja RG-02 et le journal.
 */
@Service
@PreAuthorize("hasRole('VALIDATOR')")
public class ProductReviewService {

    /** ECO-11 : toute liste est bornee. */
    public static final int PAGE_SIZE = 20;

    private final ProductRepository productRepository;

    public ProductReviewService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** US-09 : les fiches en attente de controle, les plus anciennes d'abord. */
    @Transactional(readOnly = true)
    public Page<ReviewQueueItem> queue(int page) {
        return productRepository.findQueueByStatus(
                ProductStatus.IN_REVIEW, PageRequest.of(page, PAGE_SIZE));
    }

    /**
     * US-10 CA-1 : l'integralite des donnees, en lecture seule, avant toute decision. Contrairement
     * a la consultation d'un brouillon, aucune condition d'auteur ici : le controleur doit
     * justement pouvoir ouvrir la fiche d'un autre (RG-01, « Consulter le detail : Oui »).
     */
    @Transactional(readOnly = true)
    public Product findForReview(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Fiche introuvable : " + productId));
    }
}
