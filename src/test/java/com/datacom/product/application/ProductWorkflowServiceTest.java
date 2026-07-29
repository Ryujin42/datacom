package com.datacom.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datacom.audit.domain.AuditAction;
import com.datacom.audit.domain.AuditEntry;
import com.datacom.audit.infrastructure.AuditEntryRepository;
import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import com.datacom.product.domain.UnauthorizedProductActionException;
import com.datacom.product.infrastructure.ProductRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProductWorkflowServiceTest {

    private static final Long AUTHOR = 1L;
    private static final Long VALIDATOR = 2L;
    private static final Long PRODUCT_ID = 42L;

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final AuditEntryRepository auditEntryRepository = mock(AuditEntryRepository.class);
    private final ProductWorkflowService service =
            new ProductWorkflowService(productRepository, auditEntryRepository);

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product(AUTHOR);
        product.updateIdentification("REF-001", "Produit", null);
        product.updateClassification("Categorie", null, "Fabricant", "FR");
        product.updateTraceability("LOT-1", "CERT-1", null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    }

    @Test
    void submitSavesTheProductAndRecordsAnAuditEntry() {
        service.submit(PRODUCT_ID, AUTHOR);

        verify(productRepository).save(product);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.IN_REVIEW);

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository).save(captor.capture());
        AuditEntry entry = captor.getValue();
        assertThat(entry.getAction()).isEqualTo(AuditAction.SUBMIT);
        assertThat(entry.getFromStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(entry.getToStatus()).isEqualTo(ProductStatus.IN_REVIEW);
        assertThat(entry.getUserId()).isEqualTo(AUTHOR);
        assertThat(entry.getComment()).isNull();
    }

    @Test
    void validateSavesTheProductAndRecordsAnAuditEntry() {
        product.submit(AUTHOR, java.time.Instant.now());

        service.validate(PRODUCT_ID, VALIDATOR);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.VALIDATED);
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.VALIDATE);
        assertThat(captor.getValue().getFromStatus()).isEqualTo(ProductStatus.IN_REVIEW);
        assertThat(captor.getValue().getToStatus()).isEqualTo(ProductStatus.VALIDATED);
    }

    @Test
    void returnToDraftCarriesTheOptionalCommentOnTheAuditEntry() {
        product.submit(AUTHOR, java.time.Instant.now());

        service.returnToDraft(PRODUCT_ID, VALIDATOR, "Reference incorrecte.");

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.RETURN_TO_DRAFT);
        assertThat(captor.getValue().getComment()).isEqualTo("Reference incorrecte.");
    }

    @Test
    void aSelfReviewAttemptIsRejectedBeforeAnyAuditEntryIsWritten() {
        product.submit(AUTHOR, java.time.Instant.now());

        assertThatThrownBy(() -> service.validate(PRODUCT_ID, AUTHOR))
                .isInstanceOf(UnauthorizedProductActionException.class);

        verify(auditEntryRepository, never()).save(any());
    }

    @Test
    void anUnknownProductIdFailsFastWithoutTouchingTheAuditLog() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(999L, AUTHOR))
                .isInstanceOf(NoSuchElementException.class);

        verify(auditEntryRepository, never()).save(any());
    }
}
