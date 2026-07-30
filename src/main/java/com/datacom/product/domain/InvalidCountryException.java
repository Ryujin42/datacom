package com.datacom.product.domain;

public class InvalidCountryException extends ProductInputException {

    public InvalidCountryException(String code) {
        super("Pays d'origine inconnu : \"%s\".".formatted(code));
    }
}
