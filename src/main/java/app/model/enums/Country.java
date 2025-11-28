package app.model.enums;

public enum Country {
    BULGARIA("Bulgaria"),
    AUSTRIA("Austria"),
    BELGIUM("Belgium"),
    CROATIA("Croatia"),
    CYPRUS("Cyprus"),
    CZECHIA("Czechia"),
    DENMARK("Danmark"),
    ESTONIA("Estonia"),
    FINLAND("Finland"),
    FRANCE("France"),
    GERMANY("Germany"),
    GREECE("Greece"),
    HUNGARY("Hungary"),
    IRELAND("Ireland"),
    ITALY("Italy"),
    LATVIA("Latvia"),
    LITHUANIA("Lithuania"),
    LUXEMBOURG("Luxembourg"),
    MALTA("Malta"),
    NETHERLANDS("Netherlands"),
    POLAND("Poland"),
    PORTUGAL("Portugal"),
    ROMANIA("Romania"),
    SLOVAKIA("Slovakia"),
    SLOVENIA("Slovenia"),
    SPAIN("Spain"),
    SWEDEN("Sweden"),
    UNITED_KINGDOM("United Kingdom"),
    UNITED_STATES("United States");

    private String displayName;

    Country(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
