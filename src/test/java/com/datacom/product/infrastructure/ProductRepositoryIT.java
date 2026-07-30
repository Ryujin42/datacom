package com.datacom.product.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.datacom.product.domain.Product;
import com.datacom.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Testcontainers
class ProductRepositoryIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    private Long operatorId() {
        return userRepository.findByLogin("operator1").orElseThrow().getId();
    }

    private Product completeDraft(Long createdBy, String reference) {
        Product product = new Product(createdBy);
        product.updateIdentification(reference, "Produit", null);
        product.updateClassification("Categorie", null, "Fabricant", "FR");
        product.updateTraceability("LOT-1", "CERT-1", null);
        return product;
    }

    @Test
    void theReferenceUniquenessRuleIsEnforcedByTheDatabaseRg11() {
        Long operator = operatorId();
        productRepository.save(completeDraft(operator, "REF-100"));

        Product duplicate = completeDraft(operator, "REF-100");

        assertThatThrownBy(() -> productRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aStaleWriteIsRejectedByOptimisticLockingRg07() {
        Long operator = operatorId();
        Product saved = productRepository.save(completeDraft(operator, "REF-200"));

        Product firstEditor = productRepository.findById(saved.getId()).orElseThrow();
        Product secondEditor = productRepository.findById(saved.getId()).orElseThrow();

        firstEditor.updateTraceability("LOT-1-BIS", "CERT-1", null);
        productRepository.save(firstEditor);

        secondEditor.updateTraceability("LOT-1-CONCURRENT", "CERT-1", null);

        assertThatThrownBy(() -> productRepository.save(secondEditor))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void aSavedProductCanBeReloadedByReference() {
        Long operator = operatorId();
        productRepository.save(completeDraft(operator, "REF-300"));

        assertThat(productRepository.findByReference("REF-300")).isPresent();
        assertThat(productRepository.findByReference("REF-UNKNOWN")).isEmpty();
    }
}
