package app.model.enums;

public enum TestPurchaseType {
    FORWARDING_TO_CLIENT("Forwarding to client"),
    RETURN_BACK_TO_SELLER("Return back to seller");

    private String displayName;

    TestPurchaseType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
