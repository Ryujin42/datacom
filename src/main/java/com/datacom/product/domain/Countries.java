package com.datacom.product.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Countries {

    private static final Set<String> CODES = Set.of(Locale.getISOCountries());

    private Countries() {}

    public static boolean isValid(String code) {
        return code != null && CODES.contains(code);
    }

    public static List<Country> all() {
        return CODES.stream()
                .map(
                        code ->
                                new Country(
                                        code, Locale.of("", code).getDisplayCountry(Locale.FRENCH)))
                .sorted(Comparator.comparing(Country::label))
                .toList();
    }

    public record Country(String code, String label) {}
}
