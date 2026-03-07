package com.ecommerce.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the Catalog Service.
 *
 * <h2>Security model</h2>
 * <p>The Catalog Service operates behind an API Gateway that is responsible for
 * authenticating users and validating JWTs.  Once a request passes through the
 * gateway the user's identity is propagated as the {@code X-User-Id} HTTP header.
 * Therefore, this service trusts the gateway and permits all requests so that
 * no duplicate authentication logic is needed here.</p>
 *
 * <h2>What is configured</h2>
 * <ul>
 *   <li>Stateless session management — no {@code HttpSession} is ever created.</li>
 *   <li>CSRF protection is disabled — not needed for a stateless REST API.</li>
 *   <li>HTTP Basic and form login are disabled.</li>
 *   <li>All requests are permitted (authorisation is delegated to the API Gateway).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Builds the security filter chain applied to every incoming HTTP request.
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the constructed {@link SecurityFilterChain}
     * @throws Exception if the configuration cannot be applied
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — REST APIs with stateless tokens do not need CSRF protection.
            .csrf(AbstractHttpConfigurer::disable)

            // Disable HTTP Basic authentication — the API Gateway handles auth.
            .httpBasic(AbstractHttpConfigurer::disable)

            // Disable form-based login — not applicable for a REST microservice.
            .formLogin(AbstractHttpConfigurer::disable)

            // Never create or use an HttpSession; every request is self-contained.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorisation rules:
            //   - Actuator health endpoint is always accessible.
            //   - OpenAPI/Swagger UI endpoints are always accessible.
            //   - All other requests are permitted; the API Gateway is the auth boundary.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
