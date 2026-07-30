package com.datacom.product.domain;

public class UnauthorizedProductActionException extends RuntimeException {

    public UnauthorizedProductActionException(String message) {
        super(message);
    }
}
