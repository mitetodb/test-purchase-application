package app.aop;

import java.util.regex.Pattern;

/** Masks secrets (passwords, reset tokens, keys) before method arguments/results are logged. */
public final class LogSanitizer {

    static final String MASK = "***";
    private static final Pattern SENSITIVE_NAME =
            Pattern.compile("(?i).*(password|passwd|secret|token|api[-_]?key|credential).*");
    /** key=value / key: value pairs, e.g. DTO toString() or query strings (?token=...). */
    private static final Pattern SENSITIVE_PAIR = Pattern.compile(
            "(?i)([A-Za-z_]*(?:password|passwd|secret|token|api[-_]?key|credential)[A-Za-z_]*)(\\s*[=:]\\s*)([^,;&)\\]\\s]+)");

    private LogSanitizer() {}

    public static boolean isSensitiveName(String name) {
        return name != null && SENSITIVE_NAME.matcher(name).matches();
    }

    public static String sanitize(Object value) {
        if (value == null) return "null";
        return SENSITIVE_PAIR.matcher(String.valueOf(value)).replaceAll("$1$2" + MASK);
    }

    public static String sanitizeArgs(String[] names, Object[] args) {
        if (args == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            String n = names != null && i < names.length ? names[i] : null;
            sb.append(isSensitiveName(n) ? MASK : sanitize(args[i]));
        }
        return sb.append(']').toString();
    }
}
