package com.datacom.product.application;

import com.datacom.product.domain.ProductInputException;

/** RG-11/US-07 CA-3 : le message designe la fiche qui occupe deja cette reference. */
public class DuplicateReferenceException extends ProductInputException {

    public DuplicateReferenceException(String reference, Long existingProductId) {
        super(
                "La reference « %s » est deja utilisee par la fiche n°%d."
                        .formatted(reference, existingProductId));
    }
}
