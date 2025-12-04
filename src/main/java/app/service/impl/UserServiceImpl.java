package app.service.impl;

import app.config.SecurityConfig;
import app.config.SecurityUtils;
import app.model.dto.RegistrationDTO;
import app.model.dto.UserCreateDTO;
import app.model.dto.UserEditDTO;
import app.model.dto.UserProfileDTO;
import app.model.entity.Customer;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.CustomerRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final SecurityConfig securityConfig;
    private final TestPurchaseRepository testPurchaseRepository;

    @Override
    public User register(RegistrationDTO dto) {

        if (usernameExists(dto.getUsername())) {
            throw new IllegalArgumentException("Username already taken.");
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = User.builder()
                .username(dto.getUsername())
                .password(securityConfig.passwordEncoder().encode(dto.getPassword()))
                .role(dto.getRole())
                .email(dto.getEmail())
                .country(dto.getCountry())
                .active(true)
                .build();

        user.setUpdatedByUser(null);

        return userRepository.save(user);
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Override
    public User createUser(UserCreateDTO userCreateDTO) {
        if (usernameExists(userCreateDTO.getUsername())) {
            throw new IllegalArgumentException("Username already taken.");
        }

        User user = User.builder()
                .username(userCreateDTO.getUsername())
                .password(securityConfig.passwordEncoder().encode(userCreateDTO.getPassword()))
                .email(userCreateDTO.getEmail())
                .role(userCreateDTO.getRole())
                .country(userCreateDTO.getCountry())
                .active(userCreateDTO.isActive())
                .build();

        user.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        return userRepository.save(user);
    }

    @Override
    public UserProfileDTO getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserProfileDTO dto = new UserProfileDTO();
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setCreatedOn(user.getCreatedOn());
        dto.setEmail(user.getEmail());
        dto.setImageUrl(user.getImageUrl());
        dto.setCountry(user.getCountry());

        return dto;
    }

    @Override
    public void updateProfile(String username, UserProfileDTO dto, String imageUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setEmail(dto.getEmail());
        user.setCountry(dto.getCountry());

        if (imageUrl != null) {
            user.setImageUrl(imageUrl);
        }

        user.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        userRepository.save(user);
    }

    @Override
    public void changePassword(User user, String newPassword) {
        user.setPassword(securityConfig.passwordEncoder().encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public long countActiveUsers() {
        return userRepository.countByActiveTrue();
    }

    @Override
    public long countInactiveUsers() {
        return userRepository.countByActiveFalse();
    }

    @Override
    public Map<Role, Long> countUsersByRole() {
        Map<Role, Long> result = new EnumMap<>(Role.class);
        for (Role r : Role.values()) {
            result.put(r, userRepository.countByRole(r));
        }
        return result;
    }

    @Override
    public Map<Country, Long> countUsersByCountry() {
        Map<Country, Long> result = new EnumMap<>(Country.class);
        for (Country c : Country.values()) {
            long cnt = userRepository.countByCountry(c);
            if (cnt > 0) {
                result.put(c, cnt);
            }
        }
        return result;
    }

    @Override
    public List<TestPurchase> findAllForCurrentUser() {
        User currentUser = getCurrentUser();

        Role role = currentUser.getRole();
        return switch (role) {
            case ADMIN, SALES_MANAGER -> testPurchaseRepository.findAll();

            case MYSTERY_SHOPPER ->
                    testPurchaseRepository.findByMysteryShopper_Id(currentUser.getId());

            case ACCOUNT_MANAGER ->
                    testPurchaseRepository.findForAccountManager(currentUser.getId());

            default -> List.of();
        };
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + username));
    }

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public void updateUserFromDto(UUID id, UserEditDTO dto) {
        User user = findByIdWithManagedCustomers(id);

        user.setEmail(dto.getEmail());
        user.setCountry(dto.getCountry());
        user.setRole(dto.getRole());
        user.setActive(dto.isActive());

        if (dto.getRole() == Role.ACCOUNT_MANAGER) {
            List<Customer> customers = dto.getManagedCustomerIds() == null
                    ? List.of()
                    : customerRepository.findAllById(dto.getManagedCustomerIds());
            user.setManagedCustomers(new HashSet<>(customers));
        } else {
            user.setManagedCustomers(new HashSet<>());
        }

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByIdWithManagedCustomers(UUID id) {
        return userRepository.findByIdWithManagedCustomers(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

}