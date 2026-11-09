package com.octopus.demo.admin.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "octopus.jwt")
public record JwtProperties(String secretKey, long expirationDays) {
}
