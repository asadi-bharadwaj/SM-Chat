package com.SM.ChatService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * **Summary:** Configuration class for Spring Security in the Chat Service.
 * 
 * **Flow:** Configures HTTP security to be stateless, disabling CSRF, form login, and basic auth. 
 * It implements a custom authentication mechanism using {@link org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter} 
 * to extract user identity from headers provided by the API Gateway. It also enforces internal secret validation for sensitive endpoints.
 * 
 * **Features:** Stateless header-based authentication, internal API security via shared secret, and endpoint-specific authorization.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${internal.api.secret:super-secret-internal-key}")
    private String internalSecret;

    /**
     * **Summary:** Configures the security filter chain for the application.
     * 
     * **Flow:** Defines public endpoints (WebSocket, voice messages), applies internal secret checks for purge operations, 
     * and requires authentication for all other requests. Integrates the header authentication filter.
     * 
     * **Features:** Request authorization mapping and filter orchestration.
     * 
     * @param http The {@link HttpSecurity} object to configure.
     * @return The configured {@link SecurityFilterChain}.
     * @throws Exception If an error occurs during security configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**", "/ws-sockjs/**", "/voice-messages/**").permitAll() 
                .requestMatchers("/chat/purge/**").access((authentication, context) -> {
                    HttpServletRequest request = context.getRequest();
                    String secret = request.getHeader("X-Internal-Secret");
                    return new org.springframework.security.authorization.AuthorizationDecision(internalSecret.equals(secret));
                })
                .anyRequest().authenticated()
            )
            .addFilterBefore(requestHeaderAuthenticationFilter(), RequestHeaderAuthenticationFilter.class);

        return http.build();
    }

    /**
     * **Summary:** Provides a custom entry point for authentication failures.
     * 
     * **Flow:** Returns a 401 Unauthorized error response instead of redirecting to a login page.
     * 
     * **Features:** REST-compliant security error handling.
     * 
     * @return An {@link AuthenticationEntryPoint} that sends a 401 error.
     */
    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
    }

    /**
     * **Summary:** Configures the filter for extracting identity from request headers.
     * 
     * **Flow:** Looks for the "X-Authenticated-User-Id" header and passes it to the {@link ProviderManager} 
     * to establish the security context.
     * 
     * **Features:** Gateway-integrated user identification.
     * 
     * @return A configured {@link RequestHeaderAuthenticationFilter}.
     */
    @Bean
    public RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() {
        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader("X-Authenticated-User-Id");
        filter.setExceptionIfHeaderMissing(false);
        filter.setAuthenticationManager(new ProviderManager(preAuthenticatedAuthenticationProvider()));
        return filter;
    }

    /**
     * **Summary:** Configures the authentication provider for pre-authenticated tokens.
     * 
     * **Flow:** Uses the user details service to load authorities for the user ID found in the header.
     * 
     * **Features:** Authority-based access control for pre-identified users.
     * 
     * @return A {@link PreAuthenticatedAuthenticationProvider} instance.
     */
    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(userDetailsService());
        return provider;
    }

    /**
     * **Summary:** Maps pre-authenticated user IDs to security {@link User} objects.
     * 
     * **Flow:** Extracts the principal from the token and assigns the "ROLE_USER" authority.
     * 
     * **Features:** User identity mapping and role assignment.
     * 
     * @return An {@link AuthenticationUserDetailsService} for pre-authenticated tokens.
     */
    @Bean
    public AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService() {
        return token -> new User(
                (String) token.getPrincipal(),
                "",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}