package com.datacom.audit.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datacom.audit.domain.AuditAction;
import com.datacom.audit.domain.AuditEntry;
import com.datacom.audit.infrastructure.AuditEntryRepository;
import com.datacom.product.domain.ProductStatus;
import com.datacom.product.domain.StatusTransition;
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
class AuditJournalControllerIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private WebApplicationContext context;
    @Autowired private AuditEntryRepository auditEntryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Long userId(String login) {
        return userRepository.findByLogin(login).orElseThrow().getId();
    }

    private Long ficheId(String reference) {
        Long author = userId("operator1");
        var product = new com.datacom.product.domain.Product(author);
        product.updateIdentification(reference, "Produit", null);
        return productRepository.save(product).getId();
    }

    private void decisionBy(String login, Long productId, String comment) {
        auditEntryRepository.save(
                new AuditEntry(
                        productId,
                        userId(login),
                        AuditAction.RETURN_TO_DRAFT,
                        new StatusTransition(ProductStatus.IN_REVIEW, ProductStatus.DRAFT),
                        comment,
                        Instant.now()));
    }

    @Test
    @WithUserDetails("validator1")
    void theJournalOnlyShowsTheDecisionsOfTheConnectedValidator() throws Exception {
        Long mine = ficheId("REF-900");
        Long someoneElses = ficheId("REF-901");
        decisionBy("validator1", mine, "Decision de validator1");
        decisionBy("operator1", someoneElses, "Decision d'un autre");

        mockMvc.perform(get("/journal"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Decision de validator1")))
                .andExpect(content().string(not(containsString("Decision d'un autre"))));
    }

    @Test
    @WithUserDetails("validator1")
    void theJournalCanBeFilteredByFiche() throws Exception {
        Long first = ficheId("REF-902");
        Long second = ficheId("REF-903");
        decisionBy("validator1", first, "Motif sur la premiere");
        decisionBy("validator1", second, "Motif sur la seconde");

        mockMvc.perform(get("/journal").param("fiche", String.valueOf(first)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Motif sur la premiere")))
                .andExpect(content().string(not(containsString("Motif sur la seconde"))));
    }

    @Test
    @WithUserDetails("validator1")
    void theJournalCanBeFilteredByPeriod() throws Exception {
        Long fiche = ficheId("REF-904");
        decisionBy("validator1", fiche, "Motif du jour");

        mockMvc.perform(get("/journal").param("du", "2020-01-01").param("au", "2020-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Motif du jour"))));

        mockMvc.perform(get("/journal").param("du", java.time.LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Motif du jour")));
    }

    @Test
    @WithUserDetails("operator1")
    void anOperatorCannotOpenTheJournal() throws Exception {
        mockMvc.perform(get("/journal")).andExpect(status().isForbidden());
    }
}
