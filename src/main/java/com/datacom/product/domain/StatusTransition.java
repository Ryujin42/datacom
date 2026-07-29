package com.datacom.product.domain;

/**
 * Le couple (etat avant, etat apres) d'une transition RG-04. Les deux valeurs ne circulent jamais
 * l'une sans l'autre : les porter ensemble evite qu'un appelant les inverse ou n'en renseigne
 * qu'une.
 */
public record StatusTransition(ProductStatus from, ProductStatus to) {}
