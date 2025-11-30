package app.model.dto;
public class PriceCalculationResponseDTO {

    private double testPurchaseFee;
    private double postageFee;
    private double productPrice;
    private String currency = "EUR";

    public double getTestPurchaseFee() { return testPurchaseFee; }
    public void setTestPurchaseFee(double testPurchaseFee) { this.testPurchaseFee = testPurchaseFee; }

    public double getPostageFee() { return postageFee; }
    public void setPostageFee(double postageFee) { this.postageFee = postageFee; }

    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
