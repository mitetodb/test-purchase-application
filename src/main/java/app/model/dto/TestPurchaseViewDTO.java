package app.model.dto;

import app.model.entity.*;
import app.model.enums.Category;
import app.model.enums.Country;
import app.model.enums.TestPurchaseStatus;
import app.model.enums.TestPurchaseType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * View DTO for TestPurchase that can selectively hide sensitive information
 * like mystery shopper details without mutating the underlying entity.
 */
@Data
public class TestPurchaseViewDTO {
    private UUID id;
    private String number;
    private Customer customer;
    private Shop shop;
    private Country country;
    private Category category;
    private TestPurchaseType type;
    private TestPurchaseStatus status;
    private double productPrice;
    private double serviceFee;
    private double postageFee;
    private List<Item> items;
    private User mysteryShopper;
    private boolean mysteryShopperHidden;
    private String updatedByUser;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private boolean active;

    public static TestPurchaseViewDTO from(TestPurchase purchase, boolean hideMysteryShopperName) {
        TestPurchaseViewDTO dto = new TestPurchaseViewDTO();
        dto.setId(purchase.getId());
        dto.setNumber(purchase.getNumber());
        dto.setCustomer(purchase.getCustomer());
        dto.setShop(purchase.getShop());
        dto.setCountry(purchase.getCountry());
        dto.setCategory(purchase.getCategory());
        dto.setType(purchase.getType());
        dto.setStatus(purchase.getStatus());
        dto.setProductPrice(purchase.getProductPrice());
        dto.setServiceFee(purchase.getServiceFee());
        dto.setPostageFee(purchase.getPostageFee());
        dto.setItems(purchase.getItems());
        dto.setUpdatedByUser(purchase.getUpdatedByUser());
        dto.setCreatedDate(purchase.getCreatedDate());
        dto.setLastModifiedDate(purchase.getLastModifiedDate());
        dto.setActive(purchase.isActive());
        
        if (hideMysteryShopperName) {
            dto.setMysteryShopper(null);
            dto.setMysteryShopperHidden(true);
        } else {
            dto.setMysteryShopper(purchase.getMysteryShopper());
            dto.setMysteryShopperHidden(false);
        }
        
        return dto;
    }

    public double getTotalPrice() {
        return productPrice + serviceFee + postageFee;
    }
}
