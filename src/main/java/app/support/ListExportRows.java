package app.support;

import app.model.dto.TestPurchaseViewDTO;
import app.model.entity.Customer;
import app.model.entity.User;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ListExportRows {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ListExportRows() {
    }

    public static List<List<String>> testPurchasesHeader(boolean includeUpdatedBy) {
        List<String> header = new ArrayList<>(List.of(
                "#", "Customer", "Shop", "Country", "Category", "Type", "Total Price", "Status", "Created On"));
        if (includeUpdatedBy) {
            header.add("Updated By");
        }
        return List.of(header);
    }

    public static List<String> testPurchaseRow(TestPurchaseViewDTO p, boolean includeUpdatedBy) {
        List<String> row = new ArrayList<>(List.of(
                p.getNumber(),
                p.getCustomer() != null ? p.getCustomer().getName() : "",
                p.getShop() != null ? p.getShop().getName() : "",
                p.getCountry() != null ? p.getCountry().getDisplayName() : "",
                p.getCategory() != null ? p.getCategory().getDisplayName() : "",
                p.getType() != null ? p.getType().getDisplayName() : "",
                String.valueOf(p.getProductPrice()),
                p.getStatus() != null ? p.getStatus().getDisplayName() : "",
                p.getCreatedDate() != null ? p.getCreatedDate().format(DATE_TIME) : ""
        ));
        if (includeUpdatedBy) {
            row.add(p.getUpdatedByUser() != null ? p.getUpdatedByUser() : "");
        }
        return row;
    }

    public static List<List<String>> customersHeader(boolean includeUpdatedBy) {
        List<String> header = new ArrayList<>(List.of(
                "#", "Name", "Category", "Country", "Email", "Service Fee"));
        if (includeUpdatedBy) {
            header.add("Updated By");
        }
        return List.of(header);
    }

    public static List<String> customerRow(Customer c, boolean includeUpdatedBy) {
        List<String> row = new ArrayList<>(List.of(
                c.getNumber(),
                c.getName(),
                c.getCategory() != null ? c.getCategory() : "",
                c.getCountry() != null ? c.getCountry().name() : "",
                c.getEmail() != null ? c.getEmail() : "",
                c.getBaseServiceFee() != null ? c.getBaseServiceFee().toString() : ""
        ));
        if (includeUpdatedBy) {
            row.add(c.getUpdatedByUser() != null ? c.getUpdatedByUser() : "");
        }
        return row;
    }

    public static List<List<String>> usersHeader(boolean includeUpdatedBy) {
        List<String> header = new ArrayList<>(List.of(
                "Username", "Email", "Country", "Role", "Active", "Created On"));
        if (includeUpdatedBy) {
            header.add("Updated By");
        }
        return List.of(header);
    }

    public static List<String> userRow(User u, boolean includeUpdatedBy) {
        List<String> row = new ArrayList<>(List.of(
                u.getUsername(),
                u.getEmail(),
                u.getCountry() != null ? u.getCountry().getDisplayName() : "",
                u.getRole() != null ? u.getRole().getDisplayName() : "",
                u.isActive() ? "Yes" : "No",
                u.getCreatedOn() != null ? u.getCreatedOn().format(DATE_TIME) : ""
        ));
        if (includeUpdatedBy) {
            row.add(u.getUpdatedByUser() != null ? u.getUpdatedByUser() : "");
        }
        return row;
    }
}
