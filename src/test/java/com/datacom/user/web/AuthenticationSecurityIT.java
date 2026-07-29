package com.datacom.user.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Un test par vulnerabilite de l'audit corrigee dans ce lot (QUA-02) : chacun echouerait sur le
 * code legacy (LoginServlet concatene le SQL, la deconnexion est en GET, aucune limite de
 * tentatives) et passe sur la refonte.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev") // charge les comptes de demo (db/migration/dev, V3)
@Testcontainers
class AuthenticationSecurityIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private MockMvc mockMvc;

    /** Corrige CRIT-1 : admin' -- echoue exactement comme n'importe quel identifiant inconnu. */
    @Test
    void sqlInjectionInLoginFieldFailsLikeAnyUnknownCredential() throws Exception {
        mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("username", "admin' --")
                                .param("password", "whatever"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    /**
     * RG-22/CA-2/CA-6 : meme message, qu'il s'agisse d'un identifiant inconnu ou d'un mauvais mot
     * de passe.
     */
    @Test
    void unknownAndWrongPasswordProduceTheIdenticalGenericMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(containsString("Identifiant ou mot de passe incorrect")));
    }

    /**
     * Corrige ELEV-2 : la deconnexion legacy etait en GET, declenchable par une simple balise img.
     */
    @Test
    void logoutWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/logout")).andExpect(status().isForbidden());
    }

    /** Toute page protegee redirige vers la connexion pour un visiteur anonyme. */
    @Test
    void unauthenticatedRequestToAProtectedPageRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /** SEC-05 : l'identifiant de session change apres une connexion reussie. */
    @Test
    void sessionIdChangesAfterSuccessfulLogin() throws Exception {
        MockHttpSession sessionBeforeLogin = new MockHttpSession();
        String idBeforeLogin = sessionBeforeLogin.getId();

        MvcResult result =
                mockMvc.perform(
                                post("/login")
                                        .with(csrf())
                                        .session(sessionBeforeLogin)
                                        .param("username", "operator1")
                                        .param("password", "OperatorPass123!"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();

        HttpSession sessionAfterLogin = result.getRequest().getSession(false);
        org.assertj.core.api.Assertions.assertThat(sessionAfterLogin.getId())
                .isNotEqualTo(idBeforeLogin);
    }

    /**
     * SEC-06/SEC-09 : en-tetes de durcissement presents sur une reponse ordinaire. La CSP interdit
     * tout script, ce qui neutralise un XSS stocke meme si un echappement venait a manquer.
     */
    @Test
    void hardeningHeadersArePresentOnEveryResponse() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        "Content-Security-Policy",
                                        containsString("script-src 'none'")))
                .andExpect(
                        header().string(
                                        "Content-Security-Policy",
                                        containsString("default-src 'self'")))
                .andExpect(
                        header().string(
                                        "Content-Security-Policy",
                                        containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Referrer-Policy", "same-origin"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    /** RG-22 : verrouillage apres 5 echecs sur le meme compte. */
    @Test
    void accountLocksAfterFiveFailedAttempts() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                            post("/login")
                                    .with(csrf())
                                    .param("username", "validator1")
                                    .param("password", "wrong"))
                    .andExpect(status().is3xxRedirection());
        }

        // Meme avec le bon mot de passe, le compte reste verrouille et le message reste generique.
        mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("username", "validator1")
                                .param("password", "ValidatorPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}
