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
import com.datacom.product.application.ProductReviewService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Le cycle de vie complet, vu des deux roles : l'auteur soumet, le controleur renvoie avec un
 * motif, l'auteur lit ce motif et corrige, le controleur valide. C'est le seul test qui traverse
 * DRAFT -> IN_REVIEW -> DRAFT -> IN_REVIEW -> VALIDATED, et il verifie que le journal en garde une
 * trace ordonnee et complete (RG-18 : « qui a decide du sort de ce produit, quand, et sur quelle
 * base ? »).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Testcontainers
class ReviewRoundTripIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository productRepository;
    @Autowired private AuditEntryRepository auditEntryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Long submittedFiche(String reference) {
        Long author = userRepository.findByLogin("operator1").orElseThrow().getId();
        Product product = new Product(author);
        product.updateIdentification(reference, "Produit", null);
        product.updateClassification("Categorie", null, "Fabricant", "FR");
        product.updateTraceability("LOT-1", "CERT-1", null);
        product.submit(author, Instant.now());
        return productRepository.save(product).getId();
    }

    @Test
    void aFicheReturnedWithACommentIsCorrectedByItsAuthorThenValidated() throws Exception {
        Long id = submittedFiche("REF-700");

        returnToDraftAsValidator(id);
        assertTheAuthorSeesTheCommentAndCanEditAgain(id);
        resubmitAsAuthor(id);
        validateAsValidator(id);

        Product product = productRepository.findById(id).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.VALIDATED);

        // Le journal raconte toute l'histoire, du plus recent au plus ancien.
        assertThat(auditEntryRepository.findByProductIdOrderByOccurredAtDesc(id))
                .extracting(entry -> entry.getAction())
                .containsExactly(
                        AuditAction.VALIDATE, AuditAction.SUBMIT, AuditAction.RETURN_TO_DRAFT);
    }

    private void returnToDraftAsValidator(Long id) throws Exception {
        mockMvc.perform(
                        post("/controle/" + id + "/renvoyer")
                                .with(csrf())
                                .with(as("validator1"))
                                .param("comment", "Le numero de lot ne correspond pas."))
                .andExpect(status().is3xxRedirection());
    }

    private void assertTheAuthorSeesTheCommentAndCanEditAgain(Long id) throws Exception {
        assertThat(productRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(ProductStatus.DRAFT);

        mockMvc.perform(get("/fiches/" + id + "/etape/1").with(as("operator1")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Le numero de lot ne correspond pas.")));
    }

    private void resubmitAsAuthor(Long id) throws Exception {
        long version = productRepository.findById(id).orElseThrow().getVersion();
        mockMvc.perform(
                        post("/fiches/" + id + "/etape/3")
                                .with(csrf())
                                .with(as("operator1"))
                                .param("version", String.valueOf(version))
                                .param("lotNumber", "LOT-2")
                                .param("certification", "CERT-1"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/fiches/" + id + "/soumettre").with(csrf()).with(as("operator1")))
                .andExpect(status().is3xxRedirection());
    }

    private void validateAsValidator(Long id) throws Exception {
        mockMvc.perform(post("/controle/" + id + "/valider").with(csrf()).with(as("validator1")))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * Chaque etape du parcours agit sous une identite differente, ce qu'une annotation posee sur la
     * methode de test ne permet pas d'exprimer. Les comptes sont charges par le vrai
     * UserDetailsService : les controleurs recoivent ainsi un UserPrincipal authentique, avec son
     * identifiant en base — un utilisateur simule generique n'en aurait pas, et RG-02 comme la
     * verification d'auteur porteraient sur du vide.
     */
    private RequestPostProcessor as(String login) {
        return SecurityMockMvcRequestPostProcessors.user(
                userDetailsService.loadUserByUsername(login));
    }

    /** ECO-11 : la taille de page est bornee, et cette borne est explicite. */
    @Test
    @WithUserDetails("validator1")
    void theQueueIsBoundedToTwentyPerPage() throws Exception {
        assertThat(ProductReviewService.PAGE_SIZE).isEqualTo(20);

        mockMvc.perform(get("/controle")).andExpect(status().isOk());
    }
}
