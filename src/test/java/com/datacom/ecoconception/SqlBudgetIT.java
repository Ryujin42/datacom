package com.datacom.ecoconception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datacom.product.domain.Product;
import com.datacom.product.infrastructure.ProductRepository;
import com.datacom.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * ECO-03 : au plus 3 requetes SQL par affichage, <b>independamment du volume</b>. C'est cette
 * derniere partie qui compte : chaque ecran est mesure a deux volumes, et le compte doit etre le
 * meme. Un « N+1 » tiendrait le budget sur un jeu de donnees minuscule et exploserait ensuite —
 * c'est exactement le defaut que l'audit reproche au legacy.
 *
 * <p>Le budget est verifie par un test, donc son depassement fait echouer la construction au meme
 * titre qu'une regression fonctionnelle : c'est ce que demande le §7.2 (« budgets contraignants »).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Testcontainers
class SqlBudgetIT {

    private static final int MAX_QUERIES_PER_SCREEN = 3;

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private MockMvc mockMvc;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private void createFiches(String authorLogin, int count, String referencePrefix) {
        Long author = userRepository.findByLogin(authorLogin).orElseThrow().getId();
        for (int i = 0; i < count; i++) {
            Product product = new Product(author);
            product.updateIdentification(referencePrefix + i, "Produit " + i, null);
            product.updateClassification("Categorie", null, "Fabricant", "FR");
            product.updateTraceability("LOT-1", "CERT-1", null);
            product.submit(author, Instant.now());
            productRepository.save(product);
        }
    }

    /** Nombre de requetes SQL reellement executees pendant l'affichage de l'ecran. */
    private long queriesFor(String url) throws Exception {
        statistics.clear();
        mockMvc.perform(get(url)).andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    /**
     * Mesure le meme ecran a deux volumes, tous deux <b>superieurs a une page</b>, et verifie que
     * le compte tient le budget et ne bouge pas.
     *
     * <p>Les deux volumes depassent volontairement la taille d'une page : en deca, Spring Data
     * economise la requete de comptage, si bien qu'un ecran passerait de 1 a 2 requetes pour une
     * raison qui n'a rien d'un « N+1 ». Comparer deux mesures prises sur le meme chemin d'execution
     * isole ce qu'on cherche vraiment — une requete supplementaire par ligne affichee.
     */
    private void assertScreenIsVolumeIndependent(String url, String referencePrefix)
            throws Exception {
        createFiches("operator1", 25, referencePrefix + "A-");
        long atFirstVolume = queriesFor(url);

        createFiches("operator1", 25, referencePrefix + "B-");
        long atDoubleVolume = queriesFor(url);

        assertThat(atFirstVolume)
                .as("%s : budget de %d requetes", url, MAX_QUERIES_PER_SCREEN)
                .isLessThanOrEqualTo(MAX_QUERIES_PER_SCREEN);
        assertThat(atDoubleVolume)
                .as("%s : le compte ne doit pas dependre du nombre de fiches", url)
                .isEqualTo(atFirstVolume);
    }

    @Test
    @WithUserDetails("validator1")
    void theProductListStaysWithinBudgetWhateverTheVolume() throws Exception {
        assertScreenIsVolumeIndependent("/fiches", "LIST-");
    }

    @Test
    @WithUserDetails("validator1")
    void theReviewQueueStaysWithinBudgetWhateverTheVolume() throws Exception {
        assertScreenIsVolumeIndependent("/controle", "QUEUE-");
    }

    @Test
    @WithUserDetails("validator1")
    void theHomeScreenStaysWithinBudgetWhateverTheVolume() throws Exception {
        assertScreenIsVolumeIndependent("/", "HOME-");
    }

    @Test
    @WithUserDetails("validator1")
    void theSearchScreenStaysWithinBudgetWhateverTheVolume() throws Exception {
        assertScreenIsVolumeIndependent("/fiches/recherche?terme=produit", "SEARCH-");
    }
}
