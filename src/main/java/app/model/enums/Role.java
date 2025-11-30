package app.model.enums;

public enum Role {
    ADMIN("Admin"),
    ACCOUNT_MANAGER("Account Manager"),
    SALES_MANAGER("Sales Manager"),
    CUSTOMER("Customer"),
    MYSTERY_SHOPPER("Mystery Shopper"),;

    private String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
