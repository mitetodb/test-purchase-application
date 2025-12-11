package app.service;

import app.config.SecurityConfig;
import app.model.dto.RegistrationDTO;
import app.model.dto.UserProfileDTO;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.CustomerRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SecurityConfig securityConfig;

    @Mock
    private TestPurchaseRepository testPurchaseRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.MYSTERY_SHOPPER)
                .country(Country.BULGARIA)
                .active(true)
                .build();
    }

    @Test
    void testUsernameExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        boolean exists = userService.usernameExists("testuser");

        assertThat(exists).isTrue();
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void testUsernameNotExists() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        boolean exists = userService.usernameExists("nonexistent");

        assertThat(exists).isFalse();
    }

    @Test
    void testRegister() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("newuser@example.com");
        dto.setRole(Role.MYSTERY_SHOPPER);
        dto.setCountry(Country.BULGARIA);

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(securityConfig.passwordEncoder()).thenReturn(passwordEncoder);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(dto);

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("newuser@example.com");
        assertThat(result.isActive()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUsernameAlreadyTaken() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("existinguser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");

        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already taken.");
    }

    @Test
    void testRegisterPasswordsDoNotMatch() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("different");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passwords do not match.");
    }

    @Test
    void testGetProfile() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserProfileDTO profile = userService.getProfile("testuser");

        assertThat(profile.getUsername()).isEqualTo("testuser");
        assertThat(profile.getEmail()).isEqualTo("test@example.com");
        assertThat(profile.getRole()).isEqualTo(Role.MYSTERY_SHOPPER);
    }

    @Test
    void testGetProfileUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    void testChangePassword() {
        when(securityConfig.passwordEncoder()).thenReturn(passwordEncoder);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changePassword(user, "newPassword");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCountUsers() {
        when(userRepository.count()).thenReturn(5L);

        long count = userService.countUsers();

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void testFindByUsername() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
    }

    @Test
    void testFindById() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.findById(userId);

        assertThat(result).isEqualTo(user);
    }
}

