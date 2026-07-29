package com.datacom.product.web;

/**
 * Champs recus d'une etape de saisie. Regroupes en un seul objet plutot qu'en une longue liste de
 * parametres : seuls ceux de l'etape soumise sont renseignes, les autres restent nuls et ne sont
 * jamais reecrits (US-06 CA-6).
 *
 * <p>{@code version} porte la version de la fiche telle qu'elle etait a l'ouverture du formulaire,
 * ce qui permet de detecter une modification concurrente (RG-07). {@code cible} est l'etape a
 * afficher apres enregistrement, ce qui fait de « Precedent » et « Suivant » deux enregistrements
 * comme les autres (RG-09, US-06 CA-3).
 */
public record StepSubmission(
        long version,
        Integer cible,
        String reference,
        String name,
        String description,
        String category,
        String subcategory,
        String manufacturer,
        String country,
        String lotNumber,
        String certification,
        String authorComment) {}
