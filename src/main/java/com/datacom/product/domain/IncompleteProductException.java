package com.datacom.product.domain;

public class IncompleteProductException extends ProductInputException {

    public IncompleteProductException() {
        super("La fiche ne peut pas etre soumise : des champs obligatoires sont manquants.");
    }
}
