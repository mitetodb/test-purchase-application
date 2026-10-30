package app.security;

import app.TestPurchaseApplication;
import app.model.dto.RegistrationDTO;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
import app.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class RegistrationSecurityTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void publicRegistrationShouldNotAllowPrivilegeEscalation() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("attacker");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("attacker@test.com");
        dto.setCountry(Country.GERMANY);

        User user = userService.register(dto);

        assertNotNull(user);
        assertEquals("attacker", user.getUsername());
        assertEquals(Role.MYSTERY_SHOPPER, user.getRole(), 
            "Public registration should assign MYSTERY_SHOPPER role, not allow client to choose");
        assertNotEquals(Role.ADMIN, user.getRole(), 
            "Public registration must never grant ADMIN role");
        assertNotEquals(Role.ACCOUNT_MANAGER, user.getRole(), 
            "Public registration must never grant ACCOUNT_MANAGER role");
        assertNotEquals(Role.SALES_MANAGER, user.getRole(), 
            "Public registration must never grant SALES_MANAGER role");
        assertNotEquals(Role.CUSTOMER, user.getRole(), 
            "Public registration must never grant CUSTOMER role");
    }

    @Test
    void registrationDTOShouldNotHaveRoleField() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("test@test.com");
        dto.setCountry(Country.BULGARIA);

        assertDoesNotThrow(() -> {
            User user = userService.register(dto);
            assertEquals(Role.MYSTERY_SHOPPER, user.getRole(), 
                "Server must assign role, ignoring any client input");
        });
    }

    @Test
    void multiplePublicRegistrationsShouldAllGetSameRole() {
        RegistrationDTO dto1 = new RegistrationDTO();
        dto1.setUsername("user1");
        dto1.setPassword("password123");
        dto1.setConfirmPassword("password123");
        dto1.setEmail("user1@test.com");
        dto1.setCountry(Country.AUSTRIA);

        RegistrationDTO dto2 = new RegistrationDTO();
        dto2.setUsername("user2");
        dto2.setPassword("password123");
        dto2.setConfirmPassword("password123");
        dto2.setEmail("user2@test.com");
        dto2.setCountry(Country.SPAIN);

        User user1 = userService.register(dto1);
        User user2 = userService.register(dto2);

        assertEquals(Role.MYSTERY_SHOPPER, user1.getRole());
        assertEquals(Role.MYSTERY_SHOPPER, user2.getRole());
        assertEquals(user1.getRole(), user2.getRole(), 
            "All public registrations should get the same least-privileged role");
    }
}
