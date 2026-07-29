package com.datacom.product.application;

import com.datacom.audit.domain.AuditAction;
import com.datacom.audit.domain.AuditEntry;
import com.datacom.audit.infrastructure.AuditEntryRepository;
import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import com.datacom.product.domain.StatusTransition;
import com.datacom.product.infrastructure.ProductRepository;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestre les transitions du cycle de vie d'une fiche (RG-04).
 *
 * <p>SEC-02 : le controle de role (RG-01) est porte ici, en couche service, et non par la
 * configuration des URL — une nouvelle route ou un appel interne qui oublierait la regle se heurte
 * quand meme a @PreAuthorize. L'identite de l'auteur (RG-02, RG-04) est une invariante du domaine,
 * verifiee par {@link Product} lui-meme : les deux controles sont distincts et testes separement.
 *
 * <p>US-15/CA-2 : chaque transition et l'entree d'audit qu'elle genere sont ecrites dans la meme
 * transaction.
 */
@Service
public class ProductWorkflowService {

    private final ProductRepository productRepository;
    private final AuditEntryRepository auditEntryRepository;

    public ProductWorkflowService(
            ProductRepository productRepository, AuditEntryRepository auditEntryRepository) {
        this.productRepository = productRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    @Transactional
    @PreAuthorize("hasRole('OPERATOR')")
    public void submit(Long productId, Long actingUserId) {
        Product product = getProductOrThrow(productId);
        ProductStatus from = product.getStatus();

        product.submit(actingUserId, Instant.now());

        recordTransition(product, actingUserId, AuditAction.SUBMIT, from, null);
    }

    @Transactional
    @PreAuthorize("hasRole('VALIDATOR')")
    public void validate(Long productId, Long actingUserId) {
        Product product = getProductOrThrow(productId);
        ProductStatus from = product.getStatus();

        product.validate(actingUserId, Instant.now());

        recordTransition(product, actingUserId, AuditAction.VALIDATE, from, null);
    }

    @Transactional
    @PreAuthorize("hasRole('VALIDATOR')")
    public void returnToDraft(Long productId, Long actingUserId, String comment) {
        Product product = getProductOrThrow(productId);
        ProductStatus from = product.getStatus();

        product.returnToDraft(actingUserId);

        recordTransition(product, actingUserId, AuditAction.RETURN_TO_DRAFT, from, comment);
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Fiche introuvable : " + productId));
    }

    private void recordTransition(
            Product product,
            Long actingUserId,
            AuditAction action,
            ProductStatus from,
            String comment) {
        productRepository.save(product);
        auditEntryRepository.save(
                new AuditEntry(
                        product.getId(),
                        actingUserId,
                        action,
                        new StatusTransition(from, product.getStatus()),
                        comment,
                        Instant.now()));
    }
}
