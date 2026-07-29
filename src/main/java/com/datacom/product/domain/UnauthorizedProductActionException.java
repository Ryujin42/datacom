package com.datacom.product.domain;

/**
 * RG-02/RG-04 : l'identite de l'auteur conditionne certaines transitions, independamment du role.
 */
public class UnauthorizedProductActionException extends RuntimeException {

    public UnauthorizedProductActionException(String message) {
        super(message);
    }
}
