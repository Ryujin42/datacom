package com.datacom.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchTermTest {

    @Test
    void lowercasesAndStripsAccents() {
        assertThat(SearchTerm.toLikePattern("Crème")).isEqualTo("%creme%");
        assertThat(SearchTerm.toLikePattern("ÉLÉGANT")).isEqualTo("%elegant%");
        assertThat(SearchTerm.toLikePattern("  Fabricant  ")).isEqualTo("%fabricant%");
    }

    @Test
    void escapesLikeWildcardsSoTheyAreMatchedLiterally() {
        assertThat(SearchTerm.toLikePattern("100%")).isEqualTo("%100\\%%");
        assertThat(SearchTerm.toLikePattern("a_b")).isEqualTo("%a\\_b%");
    }

    @Test
    void escapesBackslashesBeforeTheWildcardsItWouldOtherwiseCorrupt() {
        assertThat(SearchTerm.toLikePattern("a\\%b")).isEqualTo("%a\\\\\\%b%");
    }

    @Test
    void leavesQuotesUntouched() {
        assertThat(SearchTerm.toLikePattern("l'oreal")).isEqualTo("%l'oreal%");
    }
}
