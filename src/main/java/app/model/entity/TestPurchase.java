package app.model.entity;

import app.model.enums.PurchaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "test_purchases")
@Entity
public class TestPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Shop shop;

    @ManyToOne
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private PurchaseStatus status;

    private Double totalPrice;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items = new ArrayList<>();

    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    @PrePersist
    public void onCreate() {
        createdOn = LocalDateTime.now();
        status = PurchaseStatus.CREATED;
    }

    @PreUpdate
    public void onUpdate() {
        updatedOn = LocalDateTime.now();
    }
}
