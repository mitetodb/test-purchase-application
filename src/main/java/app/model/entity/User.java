package app.model.entity;

import app.model.enums.Country;
import app.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private Country country;

    private String language;
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean active = true;

    @OneToMany(mappedBy = "mysteryShopper", fetch = FetchType.LAZY)
    private List<TestPurchase> testPurchases;

    @ManyToMany
    @JoinTable(
            name = "account_manager_customers",
            joinColumns = @JoinColumn(name = "account_manager_id"),
            inverseJoinColumns = @JoinColumn(name = "customer_id")
    )
    private Set<Customer> managedCustomers = new HashSet<>();

    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    private String updatedByUser;

    @PrePersist
    public void onCreate() {
        this.createdOn = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
