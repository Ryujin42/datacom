package com.datacom.ecoconception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.datacom.product.domain.Product;
import com.datacom.product.infrastructure.ProductRepository;
import com.datacom.user.infrastructure.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
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

/**
 * ECO-01/02/04 : budgets de poids, verifies par un test pour qu'un depassement fasse echouer la
 * construction plutot que d'etre constate trop tard (§7.2, « budgets contraignants »).
 *
 * <p>Ce que ce test mesure exactement : le HTML produit, la feuille de style et les images, soit
 * l'integralite de ce qu'un navigateur telecharge — l'application ne sert aucun JavaScript et
 * n'appelle aucune ressource externe. La compression HTTP (ECO-07) n'est pas prise en compte : les
 * chiffres obtenus sont donc majorants, ce qui va dans le sens de la garantie.
 *
 * <p>Reference a battre, relevee sur le legacy (§7.2) : accueil 8,3 Mo, pages internes 2,0 Mo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Testcontainers
class PageWeightIT {

    private static final int FIRST_VISIT_BUDGET_BYTES = 300 * 1024;
    private static final int CACHED_VISIT_BUDGET_BYTES = 60 * 1024;
    private static final int IMAGES_BUDGET_BYTES = 50 * 1024;

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

    /**
     * Une page pleine : 20 lignes, soit le maximum qu'un ecran affiche (ECO-11). Le prefixe rend
     * les references uniques d'un test a l'autre — la base est partagee au sein de la classe.
     */
    private void fillAPage(String referencePrefix) {
        Long author = userRepository.findByLogin("operator1").orElseThrow().getId();
        for (int i = 0; i < 20; i++) {
            Product product = new Product(author);
            product.updateIdentification(
                    referencePrefix + i, "Produit de mesure numero " + i, "Description de mesure");
            product.updateClassification("Categorie", "Sous-categorie", "Fabricant", "FR");
            product.updateTraceability("LOT-000" + i, "CERT-0001", null);
            product.submit(author, Instant.now());
            productRepository.save(product);
        }
    }

    private int htmlWeightOf(String url) throws Exception {
        return mockMvc.perform(get(url))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .getBytes(StandardCharsets.UTF_8)
                .length;
    }

    private static int staticResourceWeight(String classpathLocation) throws IOException {
        try (InputStream stream =
                PageWeightIT.class.getResourceAsStream("/static/" + classpathLocation)) {
            assertThat(stream).as("ressource absente : " + classpathLocation).isNotNull();
            return stream.readAllBytes().length;
        }
    }

    /** ECO-04 : poids total des images. */
    @Test
    void imagesStayWithinTheirBudget() throws Exception {
        int total =
                staticResourceWeight("img/datacom.webp") + staticResourceWeight("img/datacom.png");

        assertThat(total).isLessThanOrEqualTo(IMAGES_BUDGET_BYTES);
    }

    /**
     * ECO-01 : premiere visite, tout compris — le WebP seul est telecharge, le PNG n'etant qu'un
     * repli pour les navigateurs qui ne gerent pas WebP.
     */
    @Test
    @WithUserDetails("validator1")
    void everyScreenStaysWithinTheFirstVisitBudget() throws Exception {
        fillAPage("PREM-");
        int assets = staticResourceWeight("css/app.css") + staticResourceWeight("img/datacom.webp");

        for (String url : List.of("/", "/fiches", "/controle", "/journal", "/fiches/recherche")) {
            assertThat(htmlWeightOf(url) + assets)
                    .as("poids de %s en premiere visite", url)
                    .isLessThanOrEqualTo(FIRST_VISIT_BUDGET_BYTES);
        }
    }

    /**
     * ECO-02 : visite suivante. Les ressources statiques sont servies avec une empreinte dans leur
     * nom et un cache d'un an (ECO-08), donc seule la page elle-meme repasse sur le reseau.
     */
    @Test
    @WithUserDetails("validator1")
    void everyScreenStaysWithinTheCachedVisitBudget() throws Exception {
        fillAPage("CACHE-");

        for (String url : List.of("/", "/fiches", "/controle", "/journal", "/fiches/recherche")) {
            assertThat(htmlWeightOf(url))
                    .as("poids de %s en visite suivante", url)
                    .isLessThanOrEqualTo(CACHED_VISIT_BUDGET_BYTES);
        }
    }
}
