package com.datacom.product.domain;

/** RG-14/US-07 CA-4 : un depassement est refuse avant enregistrement, jamais tronque en silence. */
public class FieldTooLongException extends RuntimeException {

    private final String field;

    public FieldTooLongException(String field, String label, int maxLength) {
        super("Le champ « %s » ne peut pas depasser %d caracteres.".formatted(label, maxLength));
        this.field = field;
    }

    /** Nom technique du champ, pour rattacher le message au bon controle a l'ecran (ERG-05). */
    public String getField() {
        return field;
    }
}
