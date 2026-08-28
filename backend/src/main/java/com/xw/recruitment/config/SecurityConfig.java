package com.xw.recruitment.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
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
                .requestMatchers(HttpMethod.POST, "/api/applications", "/api/admin/login", "/api/visits", "/api/visits/*/heartbeat").permitAll()
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
        @Value("${xw.cors.allowed-origin-patterns:}") String allowedOriginPatterns
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
            java.util.Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList()
        );
        configuration.setAllowedOriginPatterns(
            java.util.Arrays.stream(allowedOriginPatterns.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList()
        );
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private void writeUnauthorized(HttpServletResponse response) throws java.io.IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"ok\":false,\"code\":\"UNAUTHORIZED\"}");
    }
}
