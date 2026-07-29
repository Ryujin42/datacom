package com.datacom.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.datacom.audit.domain.AuditAction;
import com.datacom.audit.domain.AuditEntry;
import com.datacom.audit.infrastructure.AuditEntryRepository;
import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import com.datacom.product.domain.UnauthorizedProductActionException;
import com.datacom.product.infrastructure.ProductRepository;
import com.datacom.user.infrastructure.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * US-15/CA-2 : verifie, contre une vraie base, que chaque transition et l'entree d'audit qu'elle
 * genere sont ecrites dans la meme transaction (le journal est bien present a l'issue de l'appel,
 * pas seulement en memoire).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Testcontainers
class ProductWorkflowServiceIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private ProductWorkflowService workflowService;
    @Autowired private ProductRepository productRepository;
    @Autowired private AuditEntryRepository auditEntryRepository;
    @Autowired private UserRepository userRepository;

    private Long operatorId() {
        return userRepository.findByLogin("operator1").orElseThrow().getId();
    }

    private Long validatorId() {
        return userRepository.findByLogin("validator1").orElseThrow().getId();
    }

    private Long saveCompleteDraft(Long createdBy, String reference) {
        Product product = new Product(createdBy);
        product.updateIdentification(reference, "Produit", null);
        product.updateClassification("Categorie", null, "Fabricant", "FR");
        product.updateTraceability("LOT-1", "CERT-1", null);
        return productRepository.save(product).getId();
    }

    @Test
    void submitThenValidateWritesTheFicheAndTwoAuditEntries() {
        Long operator = operatorId();
        Long validator = validatorId();
        Long productId = saveCompleteDraft(operator, "REF-500");

        workflowService.submit(productId, operator);
        workflowService.validate(productId, validator);

        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.VALIDATED);
        assertThat(product.getValidatedBy()).isEqualTo(validator);

        List<AuditEntry> entries =
                auditEntryRepository.findByProductIdOrderByOccurredAtDesc(productId);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).getAction()).isEqualTo(AuditAction.VALIDATE);
        assertThat(entries.get(0).getFromStatus()).isEqualTo(ProductStatus.IN_REVIEW);
        assertThat(entries.get(0).getToStatus()).isEqualTo(ProductStatus.VALIDATED);
        assertThat(entries.get(0).getUserId()).isEqualTo(validator);
        assertThat(entries.get(0).getOccurredAt()).isNotNull();
        assertThat(entries.get(1).getAction()).isEqualTo(AuditAction.SUBMIT);
    }

    @Test
    void returnToDraftPersistsTheOptionalComment() {
        Long operator = operatorId();
        Long validator = validatorId();
        Long productId = saveCompleteDraft(operator, "REF-501");
        workflowService.submit(productId, operator);

        workflowService.returnToDraft(productId, validator, "Reference a corriger.");

        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);

        List<AuditEntry> entries =
                auditEntryRepository.findByProductIdOrderByOccurredAtDesc(productId);
        assertThat(entries.get(0).getAction()).isEqualTo(AuditAction.RETURN_TO_DRAFT);
        assertThat(entries.get(0).getComment()).isEqualTo("Reference a corriger.");
    }

    @Test
    void aSelfReviewAttemptLeavesNoTraceInTheAuditLogRg02() {
        Long operator = operatorId();
        Long productId = saveCompleteDraft(operator, "REF-502");
        workflowService.submit(productId, operator);

        assertThatThrownBy(() -> workflowService.validate(productId, operator))
                .isInstanceOf(UnauthorizedProductActionException.class);

        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.IN_REVIEW);
        // Seule la soumission est journalisee : la tentative de validation refusee n'a rien ecrit.
        assertThat(auditEntryRepository.findByProductIdOrderByOccurredAtDesc(productId))
                .extracting(AuditEntry::getAction)
                .containsExactly(AuditAction.SUBMIT);
    }
}
