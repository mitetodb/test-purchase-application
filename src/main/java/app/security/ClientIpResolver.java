package app.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the client IP for login rate limiting.
 * Uses {@code X-Real-IP} when set by a trusted reverse proxy (see deploy/nginx); never
 * {@code X-Forwarded-For}, which clients can spoof when appended by nginx.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
