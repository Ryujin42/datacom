package com.datacom.product.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datacom.product.domain.Product;
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
class CatalogControllerIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Long ficheOf(String authorLogin, String reference, String name, String manufacturer) {
        Long author = userRepository.findByLogin(authorLogin).orElseThrow().getId();
        Product product = new Product(author);
        product.updateIdentification(reference, name, null);
        product.updateClassification("Categorie", null, manufacturer, "FR");
        return productRepository.save(product).getId();
    }

    @Test
    @WithUserDetails("operator1")
    void anOperatorOnlySeesHisOwnFichesInTheList() throws Exception {
        ficheOf("operator1", "REF-800", "La mienne", "Acme");
        ficheOf("operator2", "REF-801", "Celle du voisin", "Acme");

        mockMvc.perform(get("/fiches"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("REF-800")))
                .andExpect(content().string(not(containsString("REF-801"))));
    }

    @Test
    @WithUserDetails("validator1")
    void aValidatorSeesEveryFiche() throws Exception {
        ficheOf("operator1", "REF-802", "Une fiche", "Acme");
        ficheOf("operator2", "REF-803", "Une autre", "Acme");

        mockMvc.perform(get("/fiches"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("REF-802")))
                .andExpect(content().string(containsString("REF-803")));
    }

    @Test
    @WithUserDetails("operator1")
    void anOperatorCannotOpenAFicheOfAnotherAuthorEvenByUrl() throws Exception {
        Long other = ficheOf("operator2", "REF-804", "Celle du voisin", "Acme");

        mockMvc.perform(get("/fiches/" + other)).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("validator1")
    void anUnknownIdGivesACleanNotFound() throws Exception {
        mockMvc.perform(get("/fiches/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.datacom"))));
    }

    @Test
    @WithUserDetails("validator1")
    void theListCanBeFilteredByStatus() throws Exception {
        Long draft = ficheOf("operator1", "REF-805", "Brouillon", "Acme");
        assertThat(draft).isNotNull();

        mockMvc.perform(get("/fiches").param("statut", "VALIDATED"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("REF-805"))));

        mockMvc.perform(get("/fiches").param("statut", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("REF-805")));
    }

    @Test
    @WithUserDetails("validator1")
    void searchIgnoresCaseAndAccents() throws Exception {
        ficheOf("operator1", "REF-806", "Un produit", "Crèmerie Générale");

        mockMvc.perform(get("/fiches/recherche").param("terme", "CREMERIE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("REF-806")));
    }

    @Test
    @WithUserDetails("validator1")
    void searchCoversReferenceAndName() throws Exception {
        ficheOf("operator1", "REF-807", "Casque antibruit", "Acme");

        mockMvc.perform(get("/fiches/recherche").param("terme", "REF-807"))
                .andExpect(content().string(containsString("Casque antibruit")));
        mockMvc.perform(get("/fiches/recherche").param("terme", "antibruit"))
                .andExpect(content().string(containsString("REF-807")));
    }

    @Test
    @WithUserDetails("validator1")
    void aPercentSignInTheSearchTermIsTreatedLiterally() throws Exception {
        ficheOf("operator1", "REF-808", "Coton 100% bio", "Acme");
        ficheOf("operator1", "REF-809", "Laine melangee", "Acme");

        mockMvc.perform(get("/fiches/recherche").param("terme", "100%"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("REF-808")))
                .andExpect(content().string(not(containsString("REF-809"))));
    }

    @Test
    @WithUserDetails("operator1")
    void anOperatorCannotSearch() throws Exception {
        mockMvc.perform(get("/fiches/recherche").param("terme", "quoi que ce soit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("operator1")
    void theDetailShowsTheTransitionHistory() throws Exception {
        Long author = userRepository.findByLogin("operator1").orElseThrow().getId();
        Product product = new Product(author);
        product.updateIdentification("REF-810", "Produit", null);
        product.updateClassification("Categorie", null, "Acme", "FR");
        product.updateTraceability("LOT-1", "CERT-1", null);
        product.submit(author, Instant.now());
        Long id = productRepository.save(product).getId();

        mockMvc.perform(get("/fiches/" + id))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Historique")));
    }
}
