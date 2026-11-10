package com.octopus.demo.admin.web;

import jakarta.servlet.http.HttpServletRequest;

/** Resolves the client address, preferring proxy headers set by the BFF. */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            for (String segment : forwardedFor.split(",")) {
                String candidate = segment.trim();
                if (!candidate.isEmpty()) {
                    return candidate;
                }
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
