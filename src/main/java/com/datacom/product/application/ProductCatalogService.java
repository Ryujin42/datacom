package com.datacom.product.application;

import com.datacom.audit.domain.AuditEntry;
import com.datacom.audit.infrastructure.AuditEntryRepository;
import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import com.datacom.product.infrastructure.ProductListItem;
import com.datacom.product.infrastructure.ProductRepository;
import com.datacom.user.domain.Role;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("isAuthenticated()")
public class ProductCatalogService {

    public static final int PAGE_SIZE = 20;

    private static final List<String> SORTABLE = List.of("reference", "name", "updatedAt");

    private final ProductRepository productRepository;
    private final AuditEntryRepository auditEntryRepository;

    public ProductCatalogService(
            ProductRepository productRepository, AuditEntryRepository auditEntryRepository) {
        this.productRepository = productRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductListItem> list(ProductScope scope, ProductStatus status, ListOrder order) {
        return productRepository.findList(scope.authorIdOrNull(), status, order.toPageable());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('VALIDATOR')")
    public Page<ProductListItem> search(String term, ListOrder order) {
        return productRepository.search(SearchTerm.toLikePattern(term), order.toPageable());
    }

    @Transactional(readOnly = true)
    public Product findForReading(Long productId, ProductScope scope) {
        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Fiche introuvable : " + productId));
        scope.ensureVisible(product);
        return product;
    }

    @Transactional(readOnly = true)
    public List<AuditEntry> history(Long productId) {
        return auditEntryRepository.findByProductIdOrderByOccurredAtDesc(productId);
    }

    @Transactional(readOnly = true)
    public HomeCounts homeCounts(Long userId, Role role) {
        long myDrafts = productRepository.countByCreatedByAndStatus(userId, ProductStatus.DRAFT);
        long awaitingReview =
                role == Role.VALIDATOR
                        ? productRepository.countByStatus(ProductStatus.IN_REVIEW)
                        : 0;
        return new HomeCounts(myDrafts, awaitingReview);
    }

    public record HomeCounts(long myDrafts, long awaitingReview) {}

    public record ProductScope(Long userId, Role role) {

        Long authorIdOrNull() {
            return role == Role.VALIDATOR ? null : userId;
        }

        void ensureVisible(Product product) {
            if (role != Role.VALIDATOR) {
                product.ensureAuthoredBy(userId);
            }
        }
    }

    public record ListOrder(int page, String sort, boolean descending) {

        public static ListOrder of(int page, String sort, boolean descending) {
            String safe = SORTABLE.contains(sort) ? sort : "updatedAt";
            return new ListOrder(Math.max(page, 0), safe, descending);
        }

        PageRequest toPageable() {
            Sort.Direction direction = descending ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, PAGE_SIZE, Sort.by(direction, sort));
        }
    }
}
