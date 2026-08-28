package com.xw.recruitment.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> {})
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/research/campaign").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/research/submissions").permitAll()
                .requestMatchers("/api/research/**").denyAll()
                .requestMatchers(HttpMethod.POST, "/api/applications", "/api/admin/login", "/api/visits", "/api/visits/*/heartbeat",
                    "/api/visits/walletcheck", "/api/visits/walletcheck/*/heartbeat").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/**", "/api/site-settings").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll())
            .exceptionHandling(errors -> errors.authenticationEntryPoint(
                (request, response, exception) -> writeUnauthorized(response)))
            .formLogin(login -> login.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.logoutUrl("/api/admin/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
            .build();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    UserDetailsService userDetailsService(
        @Value("${xw.admin.account}") String account,
        @Value("${xw.admin.password}") String password,
        PasswordEncoder encoder
    ) {
        return new InMemoryUserDetailsManager(User.withUsername(account)
            .password(encoder.encode(password))
            .roles("ADMIN")
            .build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${xw.cors.allowed-origins}") String allowedOrigins,
        @Value("${xw.cors.admin-origins}") String adminOrigins
    ) {
        List<String> exactPublicOrigins = exactOrigins(allowedOrigins, "xw.cors.allowed-origins");
        List<String> exactAdminOrigins = exactOrigins(adminOrigins, "xw.cors.admin-origins");

        CorsConfiguration publicResearch = new CorsConfiguration();
        publicResearch.setAllowedOrigins(exactPublicOrigins);
        publicResearch.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        publicResearch.setAllowedHeaders(List.of("Accept", "Content-Type"));
        publicResearch.setAllowCredentials(false);

        CorsConfiguration admin = new CorsConfiguration();
        admin.setAllowedOrigins(exactAdminOrigins);
        admin.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        admin.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        admin.setAllowCredentials(true);

        CorsConfiguration existingPublicApi = new CorsConfiguration();
        existingPublicApi.setAllowedOrigins(exactPublicOrigins);
        existingPublicApi.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        existingPublicApi.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        existingPublicApi.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/research/**", publicResearch);
        source.registerCorsConfiguration("/api/admin/**", admin);
        source.registerCorsConfiguration("/api/**", existingPublicApi);
        return source;
    }

    private List<String> exactOrigins(String configuredOrigins, String propertyName) {
        List<String> origins = Arrays.stream(configuredOrigins.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
        for (String origin : origins) {
            if (origin.contains("*")) {
                throw new IllegalStateException(propertyName + " must contain exact origins, not wildcards");
            }
            try {
                URI parsed = new URI(origin);
                boolean exactHttpOrigin = ("http".equalsIgnoreCase(parsed.getScheme())
                    || "https".equalsIgnoreCase(parsed.getScheme()))
                    && parsed.getHost() != null
                    && parsed.getUserInfo() == null
                    && (parsed.getPath() == null || parsed.getPath().isEmpty())
                    && parsed.getQuery() == null
                    && parsed.getFragment() == null;
                if (!exactHttpOrigin) {
                    throw new IllegalStateException(propertyName + " contains a non-origin value");
                }
            } catch (URISyntaxException exception) {
                throw new IllegalStateException(propertyName + " contains an invalid origin", exception);
            }
        }
        return origins;
    }

    private void writeUnauthorized(HttpServletResponse response) throws java.io.IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"ok\":false,\"code\":\"UNAUTHORIZED\"}");
    }
}
