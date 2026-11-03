package app.audit;

/** Request-scoped client IP for audit entries (set by {@link app.security.AuditContextFilter}). */
public final class AuditContext {

    private static final ThreadLocal<String> CLIENT_IP = new ThreadLocal<>();

    private AuditContext() {}

    public static void setClientIp(String clientIp) {
        CLIENT_IP.set(clientIp);
    }

    public static String getClientIp() {
        return CLIENT_IP.get();
    }

    public static void clear() {
        CLIENT_IP.remove();
    }
}
