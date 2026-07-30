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
import com.datacom.audit.infrastructure.AuditEntryRepository;
import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import com.datacom.product.infrastructure.ProductRepository;
import com.datacom.user.infrastructure.UserRepository;
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
class ProductSubmissionIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository productRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Autowired private AuditEntryRepository auditEntryRepository;
    @Autowired private UserRepository userRepository;

    private Long saveDraft(String login, String reference, boolean complete) {
        Long author = userRepository.findByLogin(login).orElseThrow().getId();
        Product product = new Product(author);
        product.updateIdentification(reference, "Produit", null);
        if (complete) {
            product.updateClassification("Categorie", null, "Fabricant", "FR");
            product.updateTraceability("LOT-1", "CERT-1", null);
        }
        return productRepository.save(product).getId();
    }

    private Product reload(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    @Test
    @WithUserDetails("operator1")
    void submittingACompleteDraftMovesItToInReviewAndRecordsAnAuditEntry() throws Exception {
        Long id = saveDraft("operator1", "REF-950", true);

        mockMvc.perform(post("/fiches/" + id + "/soumettre").with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(reload(id).getStatus()).isEqualTo(ProductStatus.IN_REVIEW);
        assertThat(auditEntryRepository.findByProductIdOrderByOccurredAtDesc(id))
                .extracting(entry -> entry.getAction())
                .containsExactly(AuditAction.SUBMIT);
    }

    @Test
    @WithUserDetails("operator1")
    void aForgedSubmissionOfAnIncompleteFicheIsRefusedAndLeavesItInDraft() throws Exception {
        Long id = saveDraft("operator1", "REF-951", false);

        mockMvc.perform(post("/fiches/" + id + "/soumettre").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("champs obligatoires sont manquants")));

        assertThat(reload(id).getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(auditEntryRepository.findByProductIdOrderByOccurredAtDesc(id)).isEmpty();
    }

    @Test
    @WithUserDetails("operator2")
    void anOperatorWhoIsNotTheAuthorCannotSubmitTheFiche() throws Exception {
        Long id = saveDraft("operator1", "REF-952", true);

        mockMvc.perform(post("/fiches/" + id + "/soumettre").with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(reload(id).getStatus()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    @WithUserDetails("operator2")
    void anOperatorCannotOpenTheDraftOfAnotherAuthor() throws Exception {
        Long id = saveDraft("operator1", "REF-953", true);

        mockMvc.perform(get("/fiches/" + id + "/etape/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("operator1")
    void aFicheInReviewCanNoLongerBeEdited() throws Exception {
        Long id = saveDraft("operator1", "REF-954", true);
        mockMvc.perform(post("/fiches/" + id + "/soumettre").with(csrf()))
                .andExpect(status().is3xxRedirection());

        long version = reload(id).getVersion();
        mockMvc.perform(
                        post("/fiches/" + id + "/etape/1")
                                .with(csrf())
                                .param("version", String.valueOf(version))
                                .param("reference", "REF-955"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("n&#39;est plus modifiable")));

        assertThat(reload(id).getReference()).isEqualTo("REF-954");
    }

    @Test
    @WithUserDetails("operator1")
    void submittingWithoutCsrfTokenIsRejected() throws Exception {
        Long id = saveDraft("operator1", "REF-956", true);

        mockMvc.perform(post("/fiches/" + id + "/soumettre")).andExpect(status().isForbidden());

        assertThat(reload(id).getStatus()).isEqualTo(ProductStatus.DRAFT);
    }
}
