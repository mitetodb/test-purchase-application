package app.support;

import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;

/**
 * Active DB rows for fixed usernames used with {@code @WithMockUser} in MVC security tests
 * (required because {@link app.security.InactiveUserSessionFilter} fails closed when no user exists).
 */
public final class TestActiveUsers {

    public static final List<String> STANDARD_USERNAMES = List.of(
            "admin", "accountmgr", "salesmgr", "customer", "mystery");

    private TestActiveUsers() {
    }

    public static void seedStandardUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        save(userRepository, passwordEncoder, "admin", Role.ADMIN);
        save(userRepository, passwordEncoder, "accountmgr", Role.ACCOUNT_MANAGER);
        save(userRepository, passwordEncoder, "salesmgr", Role.SALES_MANAGER);
        save(userRepository, passwordEncoder, "customer", Role.CUSTOMER);
        save(userRepository, passwordEncoder, "mystery", Role.MYSTERY_SHOPPER);
    }

    public static void deleteStandardUsers(UserRepository userRepository) {
        STANDARD_USERNAMES.forEach(name -> userRepository.findByUsername(name).ifPresent(u -> userRepository.deleteById(u.getId())));
    }

    private static void save(UserRepository userRepository, PasswordEncoder passwordEncoder, String username, Role role) {
        userRepository.findByUsername(username).ifPresent(u -> userRepository.deleteById(u.getId()));
        userRepository.save(User.builder()
                .username(username)
                .email(username + "@mock-user.test")
                .password(passwordEncoder.encode("password"))
                .role(role)
                .country(Country.GERMANY)
                .active(true)
                .managedCustomers(new HashSet<>())
                .build());
    }
}
