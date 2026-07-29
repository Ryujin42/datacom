package com.datacom.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static final Long AUTHOR = 1L;
    private static final Long VALIDATOR = 2L;
    private final Instant now = Instant.parse("2026-07-29T10:00:00Z");

    private Product completeDraft() {
        Product product = new Product(AUTHOR);
        product.updateIdentification("REF-001", "Produit", "Description");
        product.updateClassification("Categorie", "Sous-categorie", "Fabricant", "FR");
        product.updateTraceability("LOT-1", "CERT-1", null);
        return product;
    }

    @Test
    void startsAsDraftOnStepOne() {
        Product product = new Product(AUTHOR);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getCurrentStep()).isEqualTo((short) 1);
    }

    @Test
    void isNotCompleteWhileRequiredFieldsAreMissing() {
        Product product = new Product(AUTHOR);
        assertThat(product.isComplete()).isFalse();

        product.updateIdentification("REF-001", "Produit", null);
        assertThat(product.isComplete()).isFalse();

        product.updateClassification("Categorie", null, "Fabricant", "FR");
        assertThat(product.isComplete()).isFalse();

        product.updateTraceability("LOT-1", "CERT-1", null);
        assertThat(product.isComplete()).isTrue();
    }

    @Test
    void rejectsAReferenceThatDoesNotMatchTheImposedFormat() {
        Product product = new Product(AUTHOR);

        assertThatThrownBy(() -> product.updateIdentification("ref-001", "Produit", null))
                .isInstanceOf(InvalidReferenceFormatException.class);
    }

    @Test
    void rejectsACountryOutsideTheClosedListRg13() {
        Product product = new Product(AUTHOR);

        assertThatThrownBy(() -> product.updateClassification("Cat", null, "Fab", "ZZ"))
                .isInstanceOf(InvalidCountryException.class);
    }

    @Test
    void acceptsAnIsoCountryCode() {
        Product product = new Product(AUTHOR);

        product.updateClassification("Cat", null, "Fab", "FR");

        assertThat(product.getCountry()).isEqualTo("FR");
    }

    @Test
    void rejectsAValueLongerThanItsMaximumRg14() {
        Product product = new Product(AUTHOR);
        String tooLong = "x".repeat(Product.MAX_NAME + 1);

        assertThatThrownBy(() -> product.updateIdentification("REF-001", tooLong, null))
                .isInstanceOf(FieldTooLongException.class);

        // Rien n'a ete tronque en silence : la fiche reste intacte.
        assertThat(product.getName()).isNull();
    }

    @Test
    void navigatesFreelyBetweenTheFourSteps() {
        Product product = new Product(AUTHOR);

        product.moveToStep(3);
        assertThat(product.getCurrentStep()).isEqualTo((short) 3);
        product.moveToStep(1);
        assertThat(product.getCurrentStep()).isEqualTo((short) 1);
    }

    @Test
    void rejectsAStepOutsideTheValidRange() {
        Product product = new Product(AUTHOR);

        assertThatThrownBy(() -> product.moveToStep(5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> product.moveToStep(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitMovesACompleteDraftToInReview() {
        Product product = completeDraft();

        product.submit(AUTHOR, now);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.IN_REVIEW);
        assertThat(product.getSubmittedAt()).isEqualTo(now);
    }

    @Test
    void submitRejectsAnIncompleteDraft() {
        Product product = new Product(AUTHOR);
        product.updateIdentification("REF-001", "Produit", null);

        assertThatThrownBy(() -> product.submit(AUTHOR, now))
                .isInstanceOf(IncompleteProductException.class);
    }

    @Test
    void submitRejectsAnyoneOtherThanTheAuthor() {
        Product product = completeDraft();

        assertThatThrownBy(() -> product.submit(VALIDATOR, now))
                .isInstanceOf(UnauthorizedProductActionException.class);
    }

    @Test
    void submitRejectsAFicheThatIsNotADraft() {
        Product product = completeDraft();
        product.submit(AUTHOR, now);

        assertThatThrownBy(() -> product.submit(AUTHOR, now))
                .isInstanceOf(InvalidProductTransitionException.class);
    }

    @Test
    void editingAFicheOutsideDraftIsRejected() {
        Product product = completeDraft();
        product.submit(AUTHOR, now);

        assertThatThrownBy(() -> product.updateIdentification("REF-002", "Autre", null))
                .isInstanceOf(ProductNotEditableException.class);
        assertThatThrownBy(() -> product.moveToStep(2))
                .isInstanceOf(ProductNotEditableException.class);
    }

    @Test
    void validateMovesAnInReviewFicheToValidated() {
        Product product = completeDraft();
        product.submit(AUTHOR, now);

        product.validate(VALIDATOR, now);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.VALIDATED);
        assertThat(product.getValidatedBy()).isEqualTo(VALIDATOR);
        assertThat(product.getValidatedAt()).isEqualTo(now);
    }

    @Test
    void validateRejectsTheAuthorOfTheFicheRg02() {
        Product product = completeDraft();
        product.submit(AUTHOR, now);

        assertThatThrownBy(() -> product.validate(AUTHOR, now))
                .isInstanceOf(UnauthorizedProductActionException.class);
    }

    @Test
    void validateRejectsAFicheThatIsNotInReview() {
        Product draft = completeDraft();

        assertThatThrownBy(() -> draft.validate(VALIDATOR, now))
                .isInstanceOf(InvalidProductTransitionException.class);
    }

    @Test
    void returnToDraftMovesAnInReviewFicheBackToDraft() {
        Product product = completeDraft();
        product.submit(AUTHOR, now);

        product.returnToDraft(VALIDATOR);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void returnToDraftRejectsTheAuthorOfTheFicheRg02() {
        Product product = completeDraft();
        product.submit(AUTHOR, now);

        assertThatThrownBy(() -> product.returnToDraft(AUTHOR))
                .isInstanceOf(UnauthorizedProductActionException.class);
    }

    @Test
    void aValidatedFicheIsTerminalRg05() {
        Product product = completeDraft();
        product.submit(AUTHOR, now);
        product.validate(VALIDATOR, now);

        assertThatThrownBy(() -> product.validate(VALIDATOR, now))
                .isInstanceOf(InvalidProductTransitionException.class);
        assertThatThrownBy(() -> product.returnToDraft(VALIDATOR))
                .isInstanceOf(InvalidProductTransitionException.class);
        assertThatThrownBy(() -> product.updateIdentification("REF-002", "Autre", null))
                .isInstanceOf(ProductNotEditableException.class);
    }
}
