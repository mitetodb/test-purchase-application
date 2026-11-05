package app.service.impl;

import app.config.SecurityUtils;
import app.model.dto.RegistrationDTO;
import app.model.dto.UserCreateDTO;
import app.model.dto.UserEditDTO;
import app.model.dto.UserProfileDTO;
import app.model.entity.Customer;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.PasswordChangeKind;
import app.model.enums.Role;
import app.repository.CustomerRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.security.UserSessionInvalidationService;
import app.audit.AuditChangeSummary;
import app.model.enums.AuditAction;
import app.model.enums.AuditEntityType;
import app.service.AuditLogService;
import app.service.InviteTokenService;
import app.service.LoginSecurityService;
import app.service.PasswordPolicyService;
import app.service.UserService;
import app.model.filter.UserListFilter;
import app.repository.spec.UserSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final TestPurchaseRepository testPurchaseRepository;
    private final InviteTokenService inviteTokenService;
    private final LoginSecurityService loginSecurityService;
    private final UserSessionInvalidationService userSessionInvalidationService;
    private final AuditLogService auditLogService;
    private final PasswordPolicyService passwordPolicyService;

    @Override
    @Transactional
    public User register(RegistrationDTO dto) {
        // Validate and consume invite token FIRST before creating user
        inviteTokenService.validateAndConsumeToken(dto.getInviteToken());

        if (usernameExists(dto.getUsername())) {
            throw new IllegalArgumentException("Username already taken.");
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        passwordPolicyService.validateOrThrow(dto.getPassword());

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.MYSTERY_SHOPPER)  // Always MYSTERY_SHOPPER for public registration
                .email(dto.getEmail())
                .country(dto.getCountry())
                .active(true)
                .build();

        user.setUpdatedByUser(null);
        User savedUser = userRepository.save(user);
        inviteTokenService.recordTokenUsedBy(dto.getInviteToken(), savedUser);
        auditLogService.record(
                AuditAction.USER_CREATE,
                AuditEntityType.USER,
                savedUser.getId(),
                AuditChangeSummary.join(
                        AuditChangeSummary.kv("username", savedUser.getUsername()),
                        AuditChangeSummary.kv("role", savedUser.getRole())));
        auditLogService.record(
                AuditAction.PASSWORD_CHANGE,
                AuditEntityType.AUTH,
                savedUser.getId(),
                AuditChangeSummary.kv("source", PasswordChangeKind.INVITE_REGISTRATION));
        return savedUser;
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
    @Transactional
    public User createUser(UserCreateDTO userCreateDTO) {
        if (usernameExists(userCreateDTO.getUsername())) {
            throw new IllegalArgumentException("Username already taken.");
        }

        passwordPolicyService.validateOrThrow(userCreateDTO.getPassword());

        User user = User.builder()
                .username(userCreateDTO.getUsername())
                .password(passwordEncoder.encode(userCreateDTO.getPassword()))
                .email(userCreateDTO.getEmail())
                .role(userCreateDTO.getRole())
                .country(userCreateDTO.getCountry())
                .active(userCreateDTO.isActive())
                .build();

        user.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        User saved = userRepository.save(user);
        auditLogService.record(
                AuditAction.USER_CREATE,
                AuditEntityType.USER,
                saved.getId(),
                AuditChangeSummary.join(
                        AuditChangeSummary.kv("username", saved.getUsername()),
                        AuditChangeSummary.kv("role", saved.getRole()),
                        AuditChangeSummary.kv("active", saved.isActive())));
        return saved;
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
    @Transactional
    public void updateProfile(String username, UserProfileDTO dto, String imageUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String previousEmail = user.getEmail();
        Country previousCountry = user.getCountry();
        String previousImageUrl = user.getImageUrl();

        user.setEmail(dto.getEmail());
        user.setCountry(dto.getCountry());

        if (imageUrl != null) {
            user.setImageUrl(imageUrl);
        }

        user.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        userRepository.save(user);

        String summary = AuditChangeSummary.join(
                AuditChangeSummary.fieldChange("email", previousEmail, dto.getEmail()),
                AuditChangeSummary.fieldChange("country", previousCountry, dto.getCountry()),
                imageUrl != null
                        ? AuditChangeSummary.fieldChange("imageUrl", previousImageUrl, imageUrl)
                        : null);

        if (summary != null && !summary.isBlank()) {
            auditLogService.record(
                    AuditAction.PROFILE_UPDATE,
                    AuditEntityType.USER,
                    user.getId(),
                    summary);
        }
    }

    @Override
    @Transactional
    public void changePassword(User user, String newPassword) {
        changePassword(user, newPassword, PasswordChangeKind.USER);
    }

    @Override
    @Transactional
    public void changePassword(User user, String newPassword, PasswordChangeKind kind) {
        passwordPolicyService.validateOrThrow(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.record(
                AuditAction.PASSWORD_CHANGE,
                AuditEntityType.AUTH,
                user.getId(),
                AuditChangeSummary.kv("source", kind));
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

        Role previousRole = user.getRole();
        boolean previousActive = user.isActive();
        String previousEmail = user.getEmail();
        Country previousCountry = user.getCountry();

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

        String summary = AuditChangeSummary.join(
                AuditChangeSummary.fieldChange("email", previousEmail, dto.getEmail()),
                AuditChangeSummary.fieldChange("country", previousCountry, dto.getCountry()),
                AuditChangeSummary.fieldChange("active", previousActive, dto.isActive()));

        if (previousRole != dto.getRole()) {
            auditLogService.record(
                    AuditAction.USER_ROLE_CHANGE,
                    AuditEntityType.USER,
                    user.getId(),
                    AuditChangeSummary.fieldChange("role", previousRole, dto.getRole()));
        }
        if (summary != null && !summary.isBlank()) {
            auditLogService.record(AuditAction.USER_UPDATE, AuditEntityType.USER, user.getId(), summary);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User findByIdWithManagedCustomers(UUID id) {
        return userRepository.findByIdWithManagedCustomers(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public List<User> findAllMysteryShoppers() {
        return userRepository.findByRoleAndActive(Role.MYSTERY_SHOPPER, true);
    }

    @Override
    public Page<User> findPageForAdmin(UserListFilter filter, Pageable pageable) {
        var spec = UserSpecifications.listQuery(filter);
        return userRepository.findAll(spec, pageable);
    }

    @Override
    public long countForList(UserListFilter filter) {
        return userRepository.count(UserSpecifications.listQuery(filter));
    }

    @Override
    public List<User> findAllForListExport(UserListFilter filter, Pageable sortPageable, int maxRows) {
        var spec = UserSpecifications.listQuery(filter);
        Pageable page = PageRequest.of(0, maxRows, sortPageable.getSort());
        return userRepository.findAll(spec, page).getContent();
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setActive(false);
        userRepository.save(user);
        userSessionInvalidationService.invalidateAllSessions(user.getUsername());
        auditLogService.record(
                AuditAction.USER_DEACTIVATE,
                AuditEntityType.USER,
                user.getId(),
                AuditChangeSummary.kv("username", user.getUsername()));
    }

    @Override
    @Transactional
    public void reactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setActive(true);
        userRepository.save(user);
        auditLogService.record(
                AuditAction.USER_REACTIVATE,
                AuditEntityType.USER,
                user.getId(),
                AuditChangeSummary.kv("username", user.getUsername()));
    }

    @Override
    @Transactional
    public void unlockAccount(UUID id) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        loginSecurityService.unlockAccount(id, adminUsername);
    }
}
