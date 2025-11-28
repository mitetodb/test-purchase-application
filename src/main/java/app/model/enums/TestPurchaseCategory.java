package app.model.enums;

public enum TestPurchaseCategory {
    S("Small"),
    M("Medium"),
    L("Large"),
    XL("XLarge");

    private String displayName;

    TestPurchaseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
