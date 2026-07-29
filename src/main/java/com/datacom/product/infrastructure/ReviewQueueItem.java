package com.datacom.product.infrastructure;

import java.time.Instant;

/**
 * Les seules colonnes qu'affiche la file de controle (US-09 CA-2). C'est une projection, pas
 * l'entite : la requete ne remonte que ces champs et joint l'auteur en une passe, ce qui tient
 * l'affichage a une requete unique quel que soit le nombre de fiches (ECO-03) et evite de charger
 * des colonnes que l'ecran n'utilise pas (ECO-12).
 */
public record ReviewQueueItem(
        Long id,
        String reference,
        String name,
        String manufacturer,
        String author,
        Instant submittedAt) {}
