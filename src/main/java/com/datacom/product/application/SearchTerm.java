package com.datacom.product.application;

import java.text.Normalizer;
import java.util.Locale;

public final class SearchTerm {

    private SearchTerm() {}

    /** Le motif a passer a la requete, deja encadre de {@code %}. */
    public static String toLikePattern(String rawTerm) {
        return "%" + escapeLikeWildcards(stripAccents(rawTerm)) + "%";
    }

    private static String stripAccents(String value) {
        String lowered = value.trim().toLowerCase(Locale.FRENCH);
        return Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private static String escapeLikeWildcards(String value) {
        // L'antislash d'abord : l'echapper apres coup echapperait aussi ceux qu'on vient d'ajouter.
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
