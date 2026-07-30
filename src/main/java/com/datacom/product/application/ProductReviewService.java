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

@Service
@PreAuthorize("hasRole('VALIDATOR')")
public class ProductReviewService {

    public static final int PAGE_SIZE = 20;

    private final ProductRepository productRepository;

    public ProductReviewService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<ReviewQueueItem> queue(int page) {
        return productRepository.findQueueByStatus(
                ProductStatus.IN_REVIEW, PageRequest.of(page, PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Product findForReview(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Fiche introuvable : " + productId));
    }
}
