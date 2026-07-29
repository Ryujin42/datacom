package com.datacom.product.domain;

/** RG-12 : format impose sur la reference (3 a 32 caracteres, majuscules/chiffres/tirets). */
public class InvalidReferenceFormatException extends ProductInputException {

    public InvalidReferenceFormatException(String reference) {
        super(
                "Reference invalide : \"%s\" (format attendu : majuscules, chiffres, tirets, 3 a 32 caracteres)."
                        .formatted(reference));
    }
}
