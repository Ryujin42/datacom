package com.datacom.product.domain;

/** RG-08 : seule une fiche complete (etapes 1 a 3) peut etre soumise. */
public class IncompleteProductException extends RuntimeException {

    public IncompleteProductException() {
        super("La fiche ne peut pas etre soumise : des champs obligatoires sont manquants.");
    }
}
