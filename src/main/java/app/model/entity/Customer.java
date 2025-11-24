package app.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "customers")
@Entity
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String category;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<TestPurchase> testPurchases;

    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    @PrePersist
    public void onCreate() {
        this.createdOn = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
