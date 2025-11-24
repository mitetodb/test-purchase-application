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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean active = true;

    private LocalDateTime createdOn = LocalDateTime.now();
    private LocalDateTime updatedOn;

    public User() {}

    @PreUpdate
    public void preUpdate() {
        updatedOn = LocalDateTime.now();
    }

}
