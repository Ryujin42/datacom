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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
        log.info(
                "product {} {} {} -> {} by user {}",
                product.getId(),
                action,
                from,
                product.getStatus(),
                actingUserId);
    }
}
