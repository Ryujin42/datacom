package com.datacom.product.domain;

/** RG-13/SEC-03 : le pays d'origine appartient a une liste fermee, revalidee cote serveur. */
public class InvalidCountryException extends ProductInputException {

    public InvalidCountryException(String code) {
        super("Pays d'origine inconnu : \"%s\".".formatted(code));
    }
}
