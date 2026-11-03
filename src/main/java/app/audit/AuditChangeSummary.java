package app.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AuditChangeSummary {

    private AuditChangeSummary() {}

    public static String fieldChange(String field, Object before, Object after) {
        if (Objects.equals(before, after)) {
            return null;
        }
        return field + ":" + nullSafe(before) + "->" + nullSafe(after);
    }

    public static String join(String... parts) {
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                kept.add(part);
            }
        }
        return String.join("; ", kept);
    }

    public static String kv(String key, Object value) {
        if (value == null) {
            return null;
        }
        return key + "=" + nullSafe(value);
    }

    private static String nullSafe(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
