package com.datacom.product.domain;

public class ProductNotEditableException extends ProductInputException {

    public ProductNotEditableException(ProductStatus currentStatus) {
        super("Une fiche au statut %s n'est plus modifiable.".formatted(currentStatus));
    }
}
