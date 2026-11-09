package com.octopus.demo.admin.dto;

/** Payload for PUT /api/users/{id}/status. */
public record StatusRequest(Integer status) {
}
