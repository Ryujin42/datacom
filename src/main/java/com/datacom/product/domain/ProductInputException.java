package com.datacom.product.domain;

/**
 * Refus que l'utilisateur peut corriger lui-meme : sa saisie est invalide, ou l'action ne convient
 * pas a l'etat de la fiche. La couche web les rattrape toutes pour reafficher l'ecran avec le
 * message (US-07), la ou un refus d'autorisation donne un 403 et non un formulaire.
 *
 * <p>Distinguer les deux par le type, plutot que par une liste de {@code catch} a tenir a jour,
 * evite qu'une exception ajoutee plus tard tombe dans la mauvaise categorie par oubli.
 */
public abstract class ProductInputException extends RuntimeException {

    protected ProductInputException(String message) {
        super(message);
    }
}
