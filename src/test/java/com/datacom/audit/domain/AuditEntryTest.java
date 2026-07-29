package com.datacom.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.datacom.product.domain.ProductStatus;
import com.datacom.product.domain.StatusTransition;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditEntryTest {

    @Test
    void carriesAllTheFieldsRequiredByRg17() {
        Instant now = Instant.parse("2026-07-29T10:00:00Z");

        AuditEntry entry =
                new AuditEntry(
                        10L,
                        1L,
                        AuditAction.SUBMIT,
                        new StatusTransition(ProductStatus.DRAFT, ProductStatus.IN_REVIEW),
                        null,
                        now);

        assertThat(entry.getProductId()).isEqualTo(10L);
        assertThat(entry.getUserId()).isEqualTo(1L);
        assertThat(entry.getAction()).isEqualTo(AuditAction.SUBMIT);
        assertThat(entry.getFromStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(entry.getToStatus()).isEqualTo(ProductStatus.IN_REVIEW);
        assertThat(entry.getComment()).isNull();
        assertThat(entry.getOccurredAt()).isEqualTo(now);
    }

    @Test
    void carriesAnOptionalCommentForAReturnToDraft() {
        AuditEntry entry =
                new AuditEntry(
                        10L,
                        2L,
                        AuditAction.RETURN_TO_DRAFT,
                        new StatusTransition(ProductStatus.IN_REVIEW, ProductStatus.DRAFT),
                        "Reference incorrecte, a corriger.",
                        Instant.now());

        assertThat(entry.getComment()).isEqualTo("Reference incorrecte, a corriger.");
    }
}
