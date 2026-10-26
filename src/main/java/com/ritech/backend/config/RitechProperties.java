package com.ritech.backend.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ritech")
public record RitechProperties(Jwt jwt, Cors cors, Seed seed) {

    public record Jwt(String secret, Duration expiration) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Seed(boolean enabled, String userPassword, String managerPassword) {
    }
}
