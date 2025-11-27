package app.model.entity;

import app.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private String country;
    private String language;
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean active = true;
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
