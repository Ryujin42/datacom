package com.datacom.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CountriesTest {

    @Test
    void acceptsRealIsoCodes() {
        assertThat(Countries.isValid("FR")).isTrue();
        assertThat(Countries.isValid("DE")).isTrue();
    }

    @Test
    void rejectsAnythingOutsideTheList() {
        assertThat(Countries.isValid("ZZ")).isFalse();
        assertThat(Countries.isValid("fr")).isFalse();
        assertThat(Countries.isValid("FRA")).isFalse();
        assertThat(Countries.isValid("")).isFalse();
        assertThat(Countries.isValid(null)).isFalse();
    }

    @Test
    void exposesCodesWithFrenchLabelsSortedByLabel() {
        var all = Countries.all();

        assertThat(all).isNotEmpty();
        assertThat(all).extracting(Countries.Country::code).contains("FR", "DE", "JP");
        assertThat(all)
                .filteredOn(country -> country.code().equals("DE"))
                .singleElement()
                .extracting(Countries.Country::label)
                .isEqualTo("Allemagne");
        assertThat(all)
                .isSortedAccordingTo(java.util.Comparator.comparing(Countries.Country::label));
    }
}
