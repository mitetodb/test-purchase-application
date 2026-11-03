package app.support;

import app.model.filter.CustomerListFilter;
import app.model.filter.TestPurchaseListFilter;
import app.model.filter.UserListFilter;
import org.springframework.web.util.UriComponentsBuilder;

public final class ListFilterRedirectBuilder {

    private ListFilterRedirectBuilder() {
    }

    public static String testPurchases(TestPurchaseListFilter filter) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/testpurchases");
        if (filter == null) {
            return b.build().toUriString();
        }
        if (filter.getStatus() != null) {
            b.queryParam("status", filter.getStatus().name());
        }
        if (filter.getCustomerId() != null) {
            b.queryParam("customerId", filter.getCustomerId());
        }
        if (filter.getCreatedFrom() != null) {
            b.queryParam("createdFrom", filter.getCreatedFrom());
        }
        if (filter.getCreatedTo() != null) {
            b.queryParam("createdTo", filter.getCreatedTo());
        }
        if (filter.getMysteryShopperId() != null) {
            b.queryParam("mysteryShopperId", filter.getMysteryShopperId());
        }
        return b.build().toUriString();
    }

    public static String customers(CustomerListFilter filter) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/customers");
        if (filter == null) {
            return b.build().toUriString();
        }
        if (filter.getName() != null && !filter.getName().isBlank()) {
            b.queryParam("name", filter.getName());
        }
        if (filter.getCountry() != null) {
            b.queryParam("country", filter.getCountry().name());
        }
        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            b.queryParam("category", filter.getCategory());
        }
        if (filter.isShowDeactivated()) {
            b.queryParam("showDeactivated", true);
        }
        return b.build().toUriString();
    }

    public static String users(UserListFilter filter) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/users");
        if (filter == null) {
            return b.build().toUriString();
        }
        if (filter.getUsername() != null && !filter.getUsername().isBlank()) {
            b.queryParam("username", filter.getUsername());
        }
        if (filter.getRole() != null) {
            b.queryParam("role", filter.getRole().name());
        }
        if (filter.getActive() != null) {
            b.queryParam("active", filter.getActive());
        }
        if (filter.getCountry() != null) {
            b.queryParam("country", filter.getCountry().name());
        }
        return b.build().toUriString();
    }
}
