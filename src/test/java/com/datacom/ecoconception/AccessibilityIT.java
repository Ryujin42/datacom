package com.datacom.ecoconception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * ERG-01 a ERG-07 : ce qui se verifie mecaniquement.
 *
 * <p>Ces tests ne remplacent pas un audit RGAA — le contraste, l'ordre de tabulation ou la
 * pertinence d'un texte alternatif demandent un oeil humain. Ils verrouillent en revanche les
 * regressions les plus faciles a commettre : une page sans titre, une image sans alternative, un
 * champ sans etiquette. Chacune de ces trois erreurs a ete introduite au moins une fois dans le
 * legacy (B9 pour les titres).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Testcontainers
class AccessibilityIT {

    private static final List<String> SCREENS =
            List.of("/", "/fiches", "/controle", "/journal", "/fiches/recherche");

    private static final Pattern TITLE = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);
    private static final Pattern IMG = Pattern.compile("<img\\b[^>]*>");
    private static final Pattern INPUT_ID = Pattern.compile("<input\\b[^>]*\\bid=\"([^\"]+)\"");

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String html(String url) throws Exception {
        return mockMvc.perform(get(url)).andReturn().getResponse().getContentAsString();
    }

    /** ERG-07 et US-18 CA-3 : langue declaree, et un titre propre a chaque page (corrige B9). */
    @Test
    @WithUserDetails("validator1")
    void everyScreenDeclaresItsLanguageAndItsOwnTitle() throws Exception {
        String previousTitle = null;
        for (String url : SCREENS) {
            String page = html(url);
            assertThat(page).as("langue declaree sur %s", url).contains("<html lang=\"fr\"");

            Matcher title = TITLE.matcher(page);
            assertThat(title.find()).as("titre present sur %s", url).isTrue();
            assertThat(title.group(1)).as("titre non vide sur %s", url).isNotBlank();
            assertThat(title.group(1))
                    .as("%s ne doit pas reprendre le titre de l'ecran precedent", url)
                    .isNotEqualTo(previousTitle);
            previousTitle = title.group(1);
        }
    }

    /** ERG-01 : toute image porte une alternative textuelle. */
    @Test
    @WithUserDetails("validator1")
    void everyImageCarriesAnAlternativeText() throws Exception {
        for (String url : SCREENS) {
            Matcher images = IMG.matcher(html(url));
            while (images.find()) {
                assertThat(images.group())
                        .as("image sans alternative sur %s", url)
                        .containsPattern("alt=\"[^\"]+\"");
            }
        }
    }

    /** ERG-03 : tout champ de saisie porte une etiquette qui lui est associee. */
    @Test
    @WithUserDetails("validator1")
    void everyInputIsAssociatedWithALabel() throws Exception {
        for (String url : SCREENS) {
            String page = html(url);
            Matcher inputs = INPUT_ID.matcher(page);
            while (inputs.find()) {
                String id = inputs.group(1);
                assertThat(page)
                        .as("champ « %s » sans etiquette sur %s", id, url)
                        .contains("for=\"" + id + "\"");
            }
        }
    }

    /** ERG-02 : le lien d'evitement precede la navigation sur chaque ecran. */
    @Test
    @WithUserDetails("validator1")
    void everyScreenOffersASkipLinkPointingToItsMainContent() throws Exception {
        for (String url : SCREENS) {
            String page = html(url);
            assertThat(page).as("lien d'evitement sur %s", url).contains("href=\"#contenu\"");
            assertThat(page).as("ancre de contenu sur %s", url).contains("id=\"contenu\"");
        }
    }
}
