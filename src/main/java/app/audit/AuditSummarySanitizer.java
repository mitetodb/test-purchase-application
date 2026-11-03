package app.audit;

import app.aop.LogSanitizer;

import java.util.regex.Pattern;

/** Ensures audit change summaries never contain secrets or file payloads. */
public final class AuditSummarySanitizer {

    private static final String MASK = "***";
    private static final Pattern BASE64_LIKE = Pattern.compile("[A-Za-z0-9+/]{32,}={0,2}");
    private static final Pattern TOTP_PAIR = Pattern.compile("(?i)(totp[^=]*=)[^;\\s]+");

    private AuditSummarySanitizer() {}

    public static String sanitize(String summary) {
        if (summary == null || summary.isBlank()) {
            return "";
        }
        String s = LogSanitizer.sanitize(summary);
        s = TOTP_PAIR.matcher(s).replaceAll("$1" + MASK);
        s = redactSensitiveKeys(s);
        return truncate(s, 2000);
    }

    private static String redactSensitiveKeys(String input) {
        String[] parts = input.split(";");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String trimmed = part.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0) {
                String key = trimmed.substring(0, eq).trim();
                if (LogSanitizer.isSensitiveName(key) || key.equalsIgnoreCase("rawToken")) {
                    trimmed = key + "=" + MASK;
                } else if (key.equalsIgnoreCase("password") || key.equalsIgnoreCase("inviteToken")) {
                    trimmed = key + "=" + MASK;
                } else {
                    String value = trimmed.substring(eq + 1);
                    if (BASE64_LIKE.matcher(value).matches() && value.length() >= 40) {
                        trimmed = key + "=" + MASK;
                    }
                }
            }
            if (out.length() > 0) {
                out.append("; ");
            }
            out.append(trimmed);
        }
        return out.toString();
    }

    private static String truncate(String value, int maxLen) {
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen - 3) + "...";
    }
}
