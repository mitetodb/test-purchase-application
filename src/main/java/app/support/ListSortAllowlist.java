package app.support;

import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.Set;

/**
 * Maps request {@code sort=} property names (visible list columns) to safe JPA paths.
 */
public final class ListSortAllowlist {

    public enum ListKind {
        TEST_PURCHASE,
        CUSTOMER,
        USER
    }

    private static final Map<String, String> TEST_PURCHASE = Map.ofEntries(
            Map.entry("number", "number"),
            Map.entry("customerName", "customer.name"),
            Map.entry("shopName", "shop.name"),
            Map.entry("country", "country"),
            Map.entry("category", "category"),
            Map.entry("type", "type"),
            Map.entry("productPrice", "productPrice"),
            Map.entry("status", "status"),
            Map.entry("createdOn", "createdOn"),
            Map.entry("updatedByUser", "updatedByUser")
    );

    private static final Map<String, String> CUSTOMER = Map.ofEntries(
            Map.entry("number", "number"),
            Map.entry("name", "name"),
            Map.entry("category", "category"),
            Map.entry("country", "country"),
            Map.entry("email", "email"),
            Map.entry("baseServiceFee", "baseServiceFee"),
            Map.entry("updatedByUser", "updatedByUser")
    );

    private static final Map<String, String> USER = Map.ofEntries(
            Map.entry("username", "username"),
            Map.entry("email", "email"),
            Map.entry("country", "country"),
            Map.entry("role", "role"),
            Map.entry("active", "active"),
            Map.entry("createdOn", "createdOn"),
            Map.entry("updatedByUser", "updatedByUser")
    );

    private ListSortAllowlist() {
    }

    public static Sort sanitize(Sort requested, ListKind kind, Sort defaultSort) {
        if (requested == null || requested.isUnsorted()) {
            return defaultSort;
        }
        Map<String, String> allowed = allowlist(kind);
        var orders = requested.stream()
                .map(order -> {
                    String jpaPath = allowed.get(order.getProperty());
                    if (jpaPath == null) {
                        return null;
                    }
                    return new Sort.Order(order.getDirection(), jpaPath);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        if (orders.isEmpty()) {
            return defaultSort;
        }
        return Sort.by(orders);
    }

    public static Set<String> publicSortKeys(ListKind kind) {
        return allowlist(kind).keySet();
    }

    private static Map<String, String> allowlist(ListKind kind) {
        return switch (kind) {
            case TEST_PURCHASE -> TEST_PURCHASE;
            case CUSTOMER -> CUSTOMER;
            case USER -> USER;
        };
    }
}
