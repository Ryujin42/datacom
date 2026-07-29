package com.datacom.product.application;

import com.datacom.product.domain.ProductInputException;

/** RG-07/US-06 CA-8 : la fiche a change depuis son ouverture ; la seconde ecriture est refusee. */
public class ProductModifiedConcurrentlyException extends ProductInputException {

    public ProductModifiedConcurrentlyException() {
        super(
                "Cette fiche a ete modifiee par ailleurs depuis que vous l'avez ouverte."
                        + " Rechargez la page pour repartir de la version a jour :"
                        + " vos saisies ci-dessous n'ont pas ete enregistrees.");
    }
}
