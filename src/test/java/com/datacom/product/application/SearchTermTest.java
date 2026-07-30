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

    /** L'antislash est echappe en premier, sans quoi il echapperait les echappements ajoutes. */
    @Test
    void escapesBackslashesBeforeTheWildcardsItWouldOtherwiseCorrupt() {
        assertThat(SearchTerm.toLikePattern("a\\%b")).isEqualTo("%a\\\\\\%b%");
    }

    /** L'apostrophe n'a rien de special : la requete est parametree, jamais concatenee. */
    @Test
    void leavesQuotesUntouched() {
        assertThat(SearchTerm.toLikePattern("l'oreal")).isEqualTo("%l'oreal%");
    }
}
