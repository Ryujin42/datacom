package com.datacom.product.domain;

/** RG-05 : seule une fiche DRAFT est modifiable. */
public class ProductNotEditableException extends RuntimeException {

    public ProductNotEditableException(ProductStatus currentStatus) {
        super("Une fiche au statut %s n'est plus modifiable.".formatted(currentStatus));
    }
}
