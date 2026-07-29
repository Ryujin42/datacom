package com.datacom.product.infrastructure;

import com.datacom.product.domain.ProductStatus;
import java.time.Instant;

/**
 * Les colonnes de la liste de consultation (US-12 CA-2) et des resultats de recherche (US-14).
 * Comme {@link ReviewQueueItem}, c'est une projection : la requete ne remonte que ces champs, ce
 * qui tient l'affichage a une requete unique quel que soit le volume (ECO-03) et evite de charger
 * des colonnes inutilisees (ECO-12).
 */
public record ProductListItem(
        Long id,
        String reference,
        String name,
        ProductStatus status,
        short currentStep,
        Instant updatedAt) {}
