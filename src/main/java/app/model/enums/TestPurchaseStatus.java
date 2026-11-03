package app.model.enums;

public enum TestPurchaseStatus {
    INITIALISED("Initialised"),
    IN_PROGRESS("In Progress"),
    PRODUCT_ORDERED("Product Ordered"),
    PRODUCT_RECEIVED("Product Received"),
    PRODUCT_FORWARDED_TO_CLIENT("Product Forwarded To Client"),
    PRODUCT_SHIPPED_BACK_TO_SELLER("Product Shipped Back to Seller"),
    CLOSED("Closed"),
    CANCELLED("Cancelled"),
    ARCHIVED("Archived"),;

    public boolean isArchived() {
        return this == ARCHIVED;
    }

    private String displayName;

    TestPurchaseStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}