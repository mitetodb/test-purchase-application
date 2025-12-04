package app.repository;

import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.managedCustomers
        WHERE u.id = :id
        """)
    Optional<User> findByIdWithManagedCustomers(UUID id);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.managedCustomers
        WHERE u.username = :username
        """)
    Optional<User> findByUsernameWithManagedCustomers(String username);

    long countByActiveTrue();
    long countByActiveFalse();

    long countByRole(Role role);

    long countByCountry(Country country);
}
