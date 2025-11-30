package app.model.entity;

import app.model.enums.Country;
import app.model.enums.TestPurchaseCategory;
import app.model.enums.TestPurchaseStatus;
import app.model.enums.TestPurchaseType;
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

    @Column(name = "number", unique = true, nullable = false, length = 20)
    private String number;

    @ManyToOne
    private Shop shop;

    @ManyToOne
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private Country country;

    @Enumerated(EnumType.STRING)
    private TestPurchaseCategory category;

    @Enumerated(EnumType.STRING)
    private TestPurchaseType type;

    @Enumerated(EnumType.STRING)
    private TestPurchaseStatus status;

    private Double productPrice;
    private Double serviceFee;
    private Double postageFee;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items = new ArrayList<>();

    @OneToMany
    private List<Attachment> attachments;

    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    @PrePersist
    public void onCreate() {
        createdOn = LocalDateTime.now();
        status = TestPurchaseStatus.INITIALISED;
    }

    @PreUpdate
    public void onUpdate() {
        updatedOn = LocalDateTime.now();
    }
}
