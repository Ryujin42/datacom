package com.datacom.config;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * RG-19..RG-24, SEC-02/04/05/07/09/10. Configuration explicite (Spring Boot 4 n'active plus de
 * defauts implicites) plutot que de dependre de comportements par convention.
 */
@Configuration
public class SecurityConfig {

    /** RG-20 : bcrypt, cout >= 12. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Necessaire pour que SessionRegistry soit notifie quand une session HTTP expire. */
    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    /**
     * RG-22/CA-6 : le meme message generique s'affiche quel que soit le motif de l'echec
     * (identifiant inconnu, mot de passe errone, compte verrouille ou desactive) - aucune
     * distinction ne doit fuiter vers le client.
     */
    @Bean
    public AuthenticationFailureHandler genericAuthenticationFailureHandler() {
        return (request, response, exception) ->
                response.sendRedirect(request.getContextPath() + "/login?error");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, AuthenticationFailureHandler failureHandler) throws Exception {
        http.authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/login",
                                                "/css/**",
                                                "/img/**",
                                                "/error",
                                                "/actuator/health")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .formLogin(
                        form ->
                                form.loginPage("/login")
                                        .loginProcessingUrl("/login")
                                        .failureHandler(failureHandler)
                                        .defaultSuccessUrl("/", false)
                                        .permitAll())
                .logout(
                        logout ->
                                logout.logoutUrl("/logout")
                                        .logoutSuccessUrl("/login?logout")
                                        .invalidateHttpSession(true)
                                        .deleteCookies("JSESSIONID")
                                        .permitAll())
                .sessionManagement(
                        session ->
                                session.sessionFixation(
                                                SessionManagementConfigurer
                                                                .SessionFixationConfigurer
                                                        ::migrateSession)
                                        // -1 = pas de limite de sessions concurrentes ; on veut
                                        // seulement les
                                        // suivre via le registre pour pouvoir les invalider
                                        // explicitement
                                        // depuis US-04 (changement de mot de passe).
                                        .maximumSessions(-1)
                                        .sessionRegistry(sessionRegistry()));

        return http.build();
    }
}
