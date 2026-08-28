package com.xw.recruitment.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SecurityConfigTest {
    @Test
    void rejectsWildcardOriginsFromCredentialedConfiguration() {
        SecurityConfig config = new SecurityConfig();

        assertThrows(IllegalStateException.class, () ->
            config.corsConfigurationSource("https://public.example", "https://*.example"));
        assertThrows(IllegalStateException.class, () ->
            config.corsConfigurationSource("https://public.example", "*"));
        assertThrows(IllegalStateException.class, () ->
            config.corsConfigurationSource("https://*.example", "https://admin.example"));
    }
}
