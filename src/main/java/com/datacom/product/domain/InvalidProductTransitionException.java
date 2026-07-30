package com.datacom.product.domain;

public class InvalidProductTransitionException extends ProductInputException {

    public InvalidProductTransitionException(String action, ProductStatus currentStatus) {
        super("Impossible de %s une fiche au statut %s.".formatted(action, currentStatus));
    }
}
