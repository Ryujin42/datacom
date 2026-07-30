package com.datacom.product.application;

import com.datacom.audit.domain.AuditAction;
import com.datacom.audit.domain.AuditEntry;
import com.datacom.audit.infrastructure.AuditEntryRepository;
import com.datacom.product.domain.Product;
import com.datacom.product.infrastructure.ProductRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@PreAuthorize("hasRole('OPERATOR')")
public class ProductEditService {

    private final ProductRepository productRepository;
    private final AuditEntryRepository auditEntryRepository;

    public ProductEditService(
            ProductRepository productRepository, AuditEntryRepository auditEntryRepository) {
        this.productRepository = productRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    @Transactional(readOnly = true)
    public Optional<String> lastReturnComment(Long productId) {
        return auditEntryRepository
                .findFirstByProductIdAndActionOrderByOccurredAtDesc(
                        productId, AuditAction.RETURN_TO_DRAFT)
                .map(AuditEntry::getComment)
                .filter(comment -> !comment.isBlank());
    }

    @Transactional
    public Long create(Long authorId) {
        Long productId = productRepository.save(new Product(authorId)).getId();
        log.info("product {} created by user {}", productId, authorId);
        return productId;
    }

    @Transactional(readOnly = true)
    public Product findForAuthor(Long productId, Long actingUserId) {
        Product product = getProductOrThrow(productId);
        product.ensureAuthoredBy(actingUserId);
        return product;
    }

    @Transactional
    public void saveIdentification(
            Long productId,
            Long actingUserId,
            long expectedVersion,
            ProductStepData.Identification data) {
        Product product = openForEdit(productId, actingUserId, expectedVersion);
        ensureReferenceIsFree(data.reference(), productId);
        product.updateIdentification(data.reference(), data.name(), data.description());
        productRepository.save(product);
    }

    private void ensureReferenceIsFree(String reference, Long productId) {
        if (reference == null || reference.isBlank()) {
            return;
        }
        productRepository
                .findByReference(reference)
                .filter(existing -> !existing.getId().equals(productId))
                .ifPresent(
                        existing -> {
                            log.warn(
                                    "reference {} rejected for product {}: already used by"
                                            + " product {}",
                                    reference,
                                    productId,
                                    existing.getId());
                            throw new DuplicateReferenceException(reference, existing.getId());
                        });
    }

    @Transactional
    public void saveClassification(
            Long productId,
            Long actingUserId,
            long expectedVersion,
            ProductStepData.Classification data) {
        Product product = openForEdit(productId, actingUserId, expectedVersion);
        product.updateClassification(
                data.category(), data.subcategory(), data.manufacturer(), data.country());
        productRepository.save(product);
    }

    @Transactional
    public void saveTraceability(
            Long productId,
            Long actingUserId,
            long expectedVersion,
            ProductStepData.Traceability data) {
        Product product = openForEdit(productId, actingUserId, expectedVersion);
        product.updateTraceability(data.lotNumber(), data.certification(), data.authorComment());
        productRepository.save(product);
    }

    @Transactional
    public void moveToStep(Long productId, Long actingUserId, int step) {
        Product product = getProductOrThrow(productId);
        product.ensureAuthoredBy(actingUserId);
        product.moveToStep(step);
        productRepository.save(product);
    }

    private Product openForEdit(Long productId, Long actingUserId, long expectedVersion) {
        Product product = getProductOrThrow(productId);
        product.ensureAuthoredBy(actingUserId);
        if (product.getVersion() != expectedVersion) {
            log.warn(
                    "concurrent modification on product {}: expected version {}, found {}",
                    productId,
                    expectedVersion,
                    product.getVersion());
            throw new ProductModifiedConcurrentlyException();
        }
        return product;
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Fiche introuvable : " + productId));
    }
}
