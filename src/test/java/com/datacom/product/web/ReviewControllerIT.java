package com.datacom.product.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datacom.audit.domain.AuditAction;
import com.datacom.audit.domain.AuditEntry;
import com.datacom.audit.infrastructure.AuditEntryRepository;
import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import com.datacom.product.infrastructure.ProductRepository;
import com.datacom.user.infrastructure.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Testcontainers
class ReviewControllerIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository productRepository;
    @Autowired private AuditEntryRepository auditEntryRepository;
    @Autowired private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** Une fiche deja soumise, prete a etre controlee. */
    private Long submittedFiche(String authorLogin, String reference) {
        Long author = userRepository.findByLogin(authorLogin).orElseThrow().getId();
        Product product = new Product(author);
        product.updateIdentification(reference, "Produit", null);
        product.updateClassification("Categorie", null, "Fabricant", "FR");
        product.updateTraceability("LOT-1", "CERT-1", null);
        product.moveToStep(3);
        product.submit(author, Instant.now());
        return productRepository.save(product).getId();
    }

    private Product reload(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    @Test
    @WithUserDetails("validator1")
    void theQueueListsFichesAwaitingReviewWithTheirAuthor() throws Exception {
        submittedFiche("operator1", "REF-600");

        mockMvc.perform(get("/controle"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("REF-600")))
                .andExpect(content().string(containsString("Fabricant")))
                .andExpect(content().string(containsString("Jean Dupont")));
    }

    @Test
    @WithUserDetails("operator1")
    void anOperatorCannotOpenTheReviewQueue() throws Exception {
        mockMvc.perform(get("/controle")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("validator1")
    void theDetailScreenShowsTheDataBeforeAnyDecision() throws Exception {
        Long id = submittedFiche("operator1", "REF-601");

        mockMvc.perform(get("/controle/" + id))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("REF-601")))
                .andExpect(content().string(containsString("Valider définitivement")));
    }

    @Test
    @WithUserDetails("validator1")
    void validatingMovesTheFicheToValidatedAndRecordsWhoDecided() throws Exception {
        Long id = submittedFiche("operator1", "REF-602");
        Long validatorId = userRepository.findByLogin("validator1").orElseThrow().getId();

        mockMvc.perform(
                        post("/controle/" + id + "/valider")
                                .with(csrf())
                                .param("confirmation", "on"))
                .andExpect(status().is3xxRedirection());

        Product product = reload(id);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.VALIDATED);
        assertThat(product.getValidatedBy()).isEqualTo(validatorId);
        assertThat(product.getValidatedAt()).isNotNull();

        AuditEntry entry = auditEntryRepository.findByProductIdOrderByOccurredAtDesc(id).get(0);
        assertThat(entry.getAction()).isEqualTo(AuditAction.VALIDATE);
        assertThat(entry.getUserId()).isEqualTo(validatorId);
        assertThat(entry.getOccurredAt()).isNotNull();
    }

    @Test
    @WithUserDetails("operator1")
    void anOperatorSendingAValidationRequestGetsForbiddenAndTheFicheIsUntouched() throws Exception {
        Long id = submittedFiche("operator1", "REF-603");

        mockMvc.perform(post("/controle/" + id + "/valider").with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(reload(id).getStatus()).isEqualTo(ProductStatus.IN_REVIEW);
    }

    @Test
    @WithUserDetails("validator1")
    void aValidatorCannotValidateAFicheHeAuthoredAndIsToldWhy() throws Exception {
        Long id = submittedFiche("validator1", "REF-604");

        mockMvc.perform(post("/controle/" + id + "/valider").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RG-02")));

        assertThat(reload(id).getStatus()).isEqualTo(ProductStatus.IN_REVIEW);
        assertThat(auditEntryRepository.findByProductIdOrderByOccurredAtDesc(id)).isEmpty();
    }

    @Test
    @WithUserDetails("validator1")
    void aFicheThatIsNotInReviewCannotBeValidated() throws Exception {
        Long id = submittedFiche("operator1", "REF-605");
        mockMvc.perform(post("/controle/" + id + "/valider").with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/controle/" + id + "/valider").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Impossible de valider")));
    }

    @Test
    @WithUserDetails("validator1")
    void returningAFicheToDraftKeepsItsStepAndRecordsTheComment() throws Exception {
        Long id = submittedFiche("operator1", "REF-606");
        short stepAtSubmission = reload(id).getCurrentStep();

        mockMvc.perform(
                        post("/controle/" + id + "/renvoyer")
                                .with(csrf())
                                .param("comment", "Certification a preciser."))
                .andExpect(status().is3xxRedirection());

        Product product = reload(id);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getCurrentStep()).isEqualTo(stepAtSubmission);

        AuditEntry entry = auditEntryRepository.findByProductIdOrderByOccurredAtDesc(id).get(0);
        assertThat(entry.getAction()).isEqualTo(AuditAction.RETURN_TO_DRAFT);
        assertThat(entry.getComment()).isEqualTo("Certification a preciser.");
    }

    @Test
    @WithUserDetails("operator1")
    void anOperatorCannotReturnAFicheToDraft() throws Exception {
        Long id = submittedFiche("operator1", "REF-607");

        mockMvc.perform(post("/controle/" + id + "/renvoyer").with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(reload(id).getStatus()).isEqualTo(ProductStatus.IN_REVIEW);
    }

    @Test
    @WithUserDetails("validator1")
    void aValidatorCannotReturnAFicheHeAuthored() throws Exception {
        Long id = submittedFiche("validator1", "REF-608");

        mockMvc.perform(post("/controle/" + id + "/renvoyer").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RG-02")));

        assertThat(reload(id).getStatus()).isEqualTo(ProductStatus.IN_REVIEW);
    }

    @Test
    @WithUserDetails("validator1")
    void aDecisionWithoutCsrfTokenIsRejected() throws Exception {
        Long id = submittedFiche("operator1", "REF-609");

        mockMvc.perform(post("/controle/" + id + "/valider")).andExpect(status().isForbidden());

        assertThat(reload(id).getStatus()).isEqualTo(ProductStatus.IN_REVIEW);
    }
}
