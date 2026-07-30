package com.datacom.product.domain;

public class FieldTooLongException extends ProductInputException {

    private final String field;

    public FieldTooLongException(String field, String label, int maxLength) {
        super("Le champ « %s » ne peut pas depasser %d caracteres.".formatted(label, maxLength));
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
