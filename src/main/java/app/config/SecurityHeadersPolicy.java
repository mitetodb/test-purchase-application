package app.config;

/**
 * Central security response headers for HTML responses (Spring Security).
 * {@code frame-ancestors} in CSP replaces {@code X-Frame-Options} so the reverse proxy must not add it again.
 */
public final class SecurityHeadersPolicy {

    /**
     * Thymeleaf pages use inline {@code <script>} blocks and {@code onclick} handlers; those require
     * {@code script-src 'unsafe-inline'}. Styles use inline attributes, so {@code style-src 'unsafe-inline'}.
     * No {@code 'unsafe-eval'}.
     */
    public static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self' https://cdn.jsdelivr.net https://code.jquery.com https://cdn.datatables.net 'unsafe-inline'",
            "style-src 'self' https://cdn.jsdelivr.net https://cdn.datatables.net https://cdnjs.cloudflare.com https://fonts.googleapis.com 'unsafe-inline'",
            "font-src 'self' https://cdnjs.cloudflare.com https://fonts.gstatic.com data:",
            "img-src 'self' data: blob:",
            "media-src 'self'",
            "connect-src 'self'",
            "frame-ancestors 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'"
    );

    public static final String REFERRER_POLICY = "strict-origin-when-cross-origin";

    public static final String PERMISSIONS_POLICY = "geolocation=(), microphone=(), camera=()";

    private SecurityHeadersPolicy() {
    }
}
