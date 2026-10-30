package app.security;

import app.TestPurchaseApplication;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
import app.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class UserDeactivationSecurityTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void deactivatedUserCannotLogin() {
        // Create and save active user
        User user = new User();
        user.setUsername("testuser_deactivation");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setEmail("test@test.com");
        user.setRole(Role.MYSTERY_SHOPPER);
        user.setActive(true);
        user.setCountry(Country.GERMANY);
        user = userRepository.save(user);

        // Verify user can load (simulates successful login)
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser_deactivation");
        assertNotNull(userDetails);
        assertTrue(userDetails.isEnabled(), "Active user should be enabled");

        // Deactivate user
        userService.deactivateUser(user.getId());

        // Verify deactivated user cannot login
        UserDetails deactivatedDetails = userDetailsService.loadUserByUsername("testuser_deactivation");
        assertFalse(deactivatedDetails.isEnabled(), "Deactivated user should not be enabled");
    }

    @Test
    void userDeactivationSetsActiveToFalse() {
        User user = new User();
        user.setUsername("deactivate_test");
        user.setPassword(passwordEncoder.encode("password"));
        user.setEmail("deactivate@test.com");
        user.setRole(Role.MYSTERY_SHOPPER);
        user.setActive(true);
        user.setCountry(Country.GERMANY);
        user = userRepository.save(user);

        assertTrue(user.isActive());

        userService.deactivateUser(user.getId());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertFalse(updated.isActive(), "User should be deactivated (active=false)");
    }

    @Test
    void deactivatedUsersAreNotHardDeleted() {
        long userCountBefore = userRepository.count();

        User user = new User();
        user.setUsername("no_hard_delete");
        user.setPassword(passwordEncoder.encode("password"));
        user.setEmail("nodelete@test.com");
        user.setRole(Role.MYSTERY_SHOPPER);
        user.setActive(true);
        user.setCountry(Country.GERMANY);
        user = userRepository.save(user);

        long userCountAfterCreate = userRepository.count();
        assertEquals(userCountBefore + 1, userCountAfterCreate);

        userService.deactivateUser(user.getId());

        long userCountAfterDeactivate = userRepository.count();
        assertEquals(userCountAfterCreate, userCountAfterDeactivate, 
                "User count should not change after deactivation (no hard delete)");

        // Verify user still exists but is inactive
        User stillExists = userRepository.findById(user.getId()).orElseThrow();
        assertFalse(stillExists.isActive());
    }
}
