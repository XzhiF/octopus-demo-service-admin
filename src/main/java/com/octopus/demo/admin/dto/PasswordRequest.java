package com.octopus.demo.admin.dto;

/** Payload for PUT /api/users/{id}/password. */
public record PasswordRequest(String password) {
}
