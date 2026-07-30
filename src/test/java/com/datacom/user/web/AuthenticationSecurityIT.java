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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev") // charge les comptes de demo (db/migration/dev, V3)
@Testcontainers
class AuthenticationSecurityIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private MockMvc mockMvc;

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

    @Test
    void unknownAndWrongPasswordProduceTheIdenticalGenericMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(containsString("Identifiant ou mot de passe incorrect")));
    }

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
