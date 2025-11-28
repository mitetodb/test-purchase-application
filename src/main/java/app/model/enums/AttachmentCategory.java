package app.model.enums;

public enum AttachmentCategory {
    CURRENT_OFFER_PDF("Current Offer PDF"),
    ORDER_CONFIRMATION_PDF("Order Confirmation PDF"),
    PRODUCT_PICTURE("Product Picture"),
    INVOICE("Invoice"),
    SHIPPING_LABEL("Shipping Label"),
    RETURN_LABEL("Return Label"),
    CANCELLATION("Cancellation"),;

    private String displayName;

    AttachmentCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
