package com.datacom.product.domain;

public class InvalidReferenceFormatException extends ProductInputException {

    public InvalidReferenceFormatException(String reference) {
        super(
                "Reference invalide : \"%s\" (format attendu : majuscules, chiffres, tirets, 3 a 32 caracteres)."
                        .formatted(reference));
    }
}
