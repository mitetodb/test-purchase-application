package app.repository;

import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    long countByActiveTrue();
    long countByActiveFalse();

    long countByRole(Role role);

    long countByCountry(Country country);
}
