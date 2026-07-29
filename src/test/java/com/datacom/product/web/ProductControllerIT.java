package com.datacom.product.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import com.datacom.product.infrastructure.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * US-05 a US-08 de bout en bout, contre une vraie base : chaque test part d'une requete HTTP, comme
 * un utilisateur — ou comme un attaquant qui forge la requete sans passer par l'ecran.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Testcontainers
class ProductControllerIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository productRepository;

    private MockMvc mockMvc;

    // MockMvc est construit explicitement avec springSecurity() : sans ce configurateur, la chaine
    // de filtres s'applique mais le contexte pose par @WithUserDetails n'est jamais transmis a la
    // requete, et tout part en redirection vers /login.
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** Cree une fiche par l'IHM et rend son identifiant. */
    private Long createFiche() throws Exception {
        MvcResult result =
                mockMvc.perform(post("/fiches").with(csrf()))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrlPattern("/fiches/*/etape/1"))
                        .andReturn();
        String location = result.getResponse().getRedirectedUrl();
        return Long.valueOf(location.split("/")[2]);
    }

    private Product reload(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    @Test
    @WithUserDetails("operator1")
    void creationStartsADraftOnStepOneAttachedToItsAuthor() throws Exception {
        Long id = createFiche();

        Product product = reload(id);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getCurrentStep()).isEqualTo((short) 1);
        assertThat(product.getCreatedBy()).isNotNull();
    }

    /** US-05 CA-4 : deux creations concurrentes produisent deux fiches distinctes (corrige B2). */
    @Test
    @WithUserDetails("operator1")
    void twoCreationsProduceTwoDistinctFiches() throws Exception {
        assertThat(createFiche()).isNotEqualTo(createFiche());
    }

    /** US-05 CA-5 : un VALIDATOR ne cree pas de fiche. */
    @Test
    @WithUserDetails("validator1")
    void aValidatorCannotCreateAFiche() throws Exception {
        mockMvc.perform(post("/fiches").with(csrf())).andExpect(status().isForbidden());
    }

    /** US-06 CA-3/CA-6 : « Suivant » enregistre, et n'ecrase pas les champs des autres etapes. */
    @Test
    @WithUserDetails("operator1")
    void savingAStepKeepsTheOtherStepsIntact() throws Exception {
        Long id = createFiche();

        mockMvc.perform(
                        post("/fiches/" + id + "/etape/1")
                                .with(csrf())
                                .param("version", "0")
                                .param("cible", "2")
                                .param("reference", "REF-900")
                                .param("name", "Casque"))
                .andExpect(status().is3xxRedirection());

        Product afterStepOne = reload(id);
        mockMvc.perform(
                        post("/fiches/" + id + "/etape/2")
                                .with(csrf())
                                .param("version", String.valueOf(afterStepOne.getVersion()))
                                .param("cible", "3")
                                .param("category", "Audio")
                                .param("manufacturer", "Acme")
                                .param("country", "FR"))
                .andExpect(status().is3xxRedirection());

        Product product = reload(id);
        assertThat(product.getReference()).isEqualTo("REF-900");
        assertThat(product.getName()).isEqualTo("Casque");
        assertThat(product.getCategory()).isEqualTo("Audio");
        assertThat(product.getCurrentStep()).isEqualTo((short) 3);
    }

    /** US-06 CA-5 : un champ vide s'affiche vide, jamais « null » (corrige B5). */
    @Test
    @WithUserDetails("operator1")
    void emptyFieldsAreNeverRenderedAsTheStringNull() throws Exception {
        Long id = createFiche();

        mockMvc.perform(get("/fiches/" + id + "/etape/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("null"))));
    }

    /** US-06 CA-8/RG-07 : une version perimee est refusee, avec un message explicite. */
    @Test
    @WithUserDetails("operator1")
    void aStaleVersionIsRejectedWithAConflictMessage() throws Exception {
        Long id = createFiche();
        mockMvc.perform(
                        post("/fiches/" + id + "/etape/1")
                                .with(csrf())
                                .param("version", "0")
                                .param("reference", "REF-901"))
                .andExpect(status().is3xxRedirection());

        // Seconde ecriture avec la version telle qu'a l'ouverture initiale : perimee.
        mockMvc.perform(
                        post("/fiches/" + id + "/etape/1")
                                .with(csrf())
                                .param("version", "0")
                                .param("reference", "REF-902"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("modifiee par ailleurs")));

        assertThat(reload(id).getReference()).isEqualTo("REF-901");
    }

    /** US-07 CA-2 : format de reference invalide refuse, avec rappel du format. */
    @Test
    @WithUserDetails("operator1")
    void anInvalidReferenceFormatIsRejectedAndTheTypedValueIsKept() throws Exception {
        Long id = createFiche();

        mockMvc.perform(
                        post("/fiches/" + id + "/etape/1")
                                .with(csrf())
                                .param("version", "0")
                                .param("reference", "ref minuscule")
                                .param("name", "Casque"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reference invalide")))
                // US-07 CA-6 : la saisie de l'utilisateur est conservee a l'ecran.
                .andExpect(content().string(containsString("ref minuscule")));

        assertThat(reload(id).getReference()).isNull();
    }

    /** US-07 CA-5/SEC-03 : un pays hors liste envoye directement en requete est refuse. */
    @Test
    @WithUserDetails("operator1")
    void aForgedCountryOutsideTheClosedListIsRejectedServerSide() throws Exception {
        Long id = createFiche();

        mockMvc.perform(
                        post("/fiches/" + id + "/etape/2")
                                .with(csrf())
                                .param("version", "0")
                                .param("category", "Audio")
                                .param("manufacturer", "Acme")
                                .param("country", "ZZ"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pays d&#39;origine inconnu")));

        assertThat(reload(id).getCountry()).isNull();
    }

    /** US-07 CA-4/SEC-03 : un depassement de longueur est refuse, sans troncature silencieuse. */
    @Test
    @WithUserDetails("operator1")
    void aForgedOversizedValueIsRejectedWithoutSilentTruncation() throws Exception {
        Long id = createFiche();

        mockMvc.perform(
                        post("/fiches/" + id + "/etape/1")
                                .with(csrf())
                                .param("version", "0")
                                .param("reference", "REF-903")
                                .param("name", "x".repeat(Product.MAX_NAME + 1)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ne peut pas depasser")));

        assertThat(reload(id).getName()).isNull();
    }

    /**
     * SEC-06 : un script stocke dans un champ texte ressort echappe, jamais executable. La CSP
     * (script-src 'none') constitue la seconde barriere, verifiee par AuthenticationSecurityIT.
     */
    @Test
    @WithUserDetails("operator1")
    void aStoredScriptIsEscapedOnOutput() throws Exception {
        Long id = createFiche();
        mockMvc.perform(
                        post("/fiches/" + id + "/etape/1")
                                .with(csrf())
                                .param("version", "0")
                                .param("reference", "REF-904")
                                .param("name", "<script>alert(1)</script>"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/fiches/" + id + "/etape/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }
}
