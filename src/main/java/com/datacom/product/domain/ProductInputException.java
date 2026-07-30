package com.datacom.product.domain;

public abstract class ProductInputException extends RuntimeException {

    protected ProductInputException(String message) {
        super(message);
    }
}
