package com.datacom.product.application;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Traduit ce que l'utilisateur a tape en motif {@code LIKE}.
 *
 * <p>US-14 CA-4/SEC-01 : {@code %}, {@code _} et {@code \} sont des caracteres speciaux de LIKE. Un
 * utilisateur qui cherche « 100% coton » cherche ce texte, pas « tout ce qui commence par 100 » ;
 * ils sont donc echappes et traites litteralement. L'apostrophe, elle, n'a rien de particulier ici
 * — la requete est parametree, jamais concatenee — mais CA-4 demande de le verifier explicitement.
 *
 * <p>CA-2 : le terme est mis en minuscules et depouille de ses accents pour correspondre a la
 * colonne generee {@code search_text}, qui subit le meme traitement cote base.
 */
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
