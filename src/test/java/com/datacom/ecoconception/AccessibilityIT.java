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
