package com.datacom.product.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.datacom.product.domain.Product;
import com.datacom.product.infrastructure.ProductRepository;
import com.datacom.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * SEC-02 : l'autorisation est refusee par defaut et portee par la couche service. Chaque methode
 * est appelee directement, sans passer par une URL : ce qui protege l'action n'est pas la
 * configuration des routes mais l'annotation sur le service, donc une future route qui oublierait
 * la regle reste bloquee.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Testcontainers
class ProductAuthorizationIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private ProductWorkflowService workflowService;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    private Long saveDraft(String reference) {
        Long author = userRepository.findByLogin("operator1").orElseThrow().getId();
        Product product = new Product(author);
        product.updateIdentification(reference, "Produit", null);
        return productRepository.save(product).getId();
    }

    @Test
    @WithMockUser(roles = "VALIDATOR")
    void aValidatorCannotSubmitAFiche() {
        Long productId = saveDraft("REF-700");

        assertThatThrownBy(() -> workflowService.submit(productId, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void anOperatorCannotValidateAFiche() {
        Long productId = saveDraft("REF-701");

        assertThatThrownBy(() -> workflowService.validate(productId, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void anOperatorCannotReturnAFicheToDraft() {
        Long productId = saveDraft("REF-702");

        assertThatThrownBy(() -> workflowService.returnToDraft(productId, 1L, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Sans aucun contexte d'authentification, le refus est signale par
     * AuthenticationCredentialsNotFoundException et non AccessDeniedException : la distinction n'a
     * pas d'importance ici, seul compte le fait que l'appel n'aboutit pas — c'est le « refuse par
     * defaut » de SEC-02.
     */
    @Test
    void anAnonymousCallerIsDeniedByDefault() {
        Long productId = saveDraft("REF-703");

        assertThatThrownBy(() -> workflowService.submit(productId, 1L))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
