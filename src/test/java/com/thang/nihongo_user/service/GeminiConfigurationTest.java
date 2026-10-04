package com.thang.nihongo_user.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeminiConfigurationTest {
    private StandardEnvironment environment(Map<String, Object> local, Map<String, Object> variables) throws Exception {
        var environment = new StandardEnvironment();
        var sources = environment.getPropertySources();
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.addLast(new SystemEnvironmentPropertySource("environment", variables));
        sources.addLast(new MapPropertySource("local", local));
        for (var source : new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"))) {
            sources.addLast(source);
        }
        return environment;
    }

    @Test
    void localKeyOverridesStaleEnvironmentKey() throws Exception {
        var environment = environment(Map.of("gemini.local-api-key", "authorized-local-key"),
                Map.of("GEMINI_API_KEY", "blocked-environment-key"));
        assertEquals("authorized-local-key", environment.getProperty("gemini.access-key"));
    }

    @Test
    void keepsEnvironmentConfigurationWhenNoLocalKeyIsSet() throws Exception {
        assertEquals("environment-key", environment(Map.of(), Map.of("GEMINI_API_KEY", "environment-key"))
                .getProperty("gemini.access-key"));
    }

    @Test
    void keepsLegacyPropertyConfiguration() throws Exception {
        assertEquals("legacy-key", environment(Map.of("gemini.api-key", "legacy-key"), Map.of())
                .getProperty("gemini.access-key"));
    }
}
