package com.datacom.product.application;

import com.datacom.product.domain.ProductInputException;

public class DuplicateReferenceException extends ProductInputException {

    public DuplicateReferenceException(String reference, Long existingProductId) {
        super(
                "La reference « %s » est deja utilisee par la fiche n°%d."
                        .formatted(reference, existingProductId));
    }
}
