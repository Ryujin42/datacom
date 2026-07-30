package com.datacom.product.infrastructure;

import com.datacom.product.domain.ProductStatus;
import java.time.Instant;

public record ProductListItem(
        Long id,
        String reference,
        String name,
        ProductStatus status,
        short currentStep,
        Instant updatedAt) {}
