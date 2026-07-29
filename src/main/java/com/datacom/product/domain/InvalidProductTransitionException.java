package com.datacom.product.domain;

/** RG-04 : seules les trois transitions du cycle de vie existent. */
public class InvalidProductTransitionException extends RuntimeException {

    public InvalidProductTransitionException(String action, ProductStatus currentStatus) {
        super("Impossible de %s une fiche au statut %s.".formatted(action, currentStatus));
    }
}
