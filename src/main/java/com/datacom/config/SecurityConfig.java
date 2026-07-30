package com.datacom.config;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Bean
    public AuthenticationFailureHandler genericAuthenticationFailureHandler() {
        return (request, response, exception) ->
                response.sendRedirect(request.getContextPath() + "/login?error");
    }

    private static void hardenResponseHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers.contentSecurityPolicy(
                        csp ->
                                csp.policyDirectives(
                                        "default-src 'self'; "
                                                + "script-src 'none'; "
                                                + "style-src 'self'; "
                                                + "img-src 'self'; "
                                                + "form-action 'self'; "
                                                + "frame-ancestors 'none'; "
                                                + "base-uri 'none'; "
                                                + "object-src 'none'"))
                .referrerPolicy(
                        referrer ->
                                referrer.policy(
                                        ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN));
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
                .headers(SecurityConfig::hardenResponseHeaders)
                .sessionManagement(this::configureSessionManagement);

        return http.build();
    }

    private void configureSessionManagement(SessionManagementConfigurer<HttpSecurity> session) {
        session.sessionFixation(
                        SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry());
    }
}
