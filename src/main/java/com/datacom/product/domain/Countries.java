package com.datacom.product.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * RG-13 : liste fermee des codes pays ISO 3166-1 alpha-2, presentee a l'utilisateur en clair.
 *
 * <p>La liste vient de {@link Locale#getISOCountries()}, c'est-a-dire de la norme telle que le JDK
 * l'embarque, plutot que d'une copie figee dans le depot : pas de recopie de 249 codes a maintenir,
 * pas de faute de frappe possible, et les libelles francais sont obtenus par la meme voie.
 */
public final class Countries {

    private static final Set<String> CODES = Set.of(Locale.getISOCountries());

    private Countries() {}

    /** SEC-03 : appele cote serveur, une valeur forgee hors liste est refusee. */
    public static boolean isValid(String code) {
        return code != null && CODES.contains(code);
    }

    /** Codes et libelles francais, tries par libelle, pour alimenter la liste deroulante. */
    public static List<Country> all() {
        return CODES.stream()
                .map(
                        code ->
                                new Country(
                                        code, Locale.of("", code).getDisplayCountry(Locale.FRENCH)))
                .sorted(Comparator.comparing(Country::label))
                .toList();
    }

    /** Un code ISO et son libelle affichable. */
    public record Country(String code, String label) {}
}
