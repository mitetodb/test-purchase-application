package app.security;

import app.TestPurchaseApplication;
import app.audit.AuditContext;
import app.model.dto.CustomerDTO;
import app.model.dto.ShopDTO;
import app.model.dto.TestPurchaseCreateDTO;
import app.model.dto.UserCreateDTO;
import app.model.dto.UserEditDTO;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.User;
import app.model.enums.AuditAction;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.AuditLogRepository;
import app.repository.CustomerRepository;
import app.repository.InviteTokenRepository;
import app.repository.ShopRepository;
import app.repository.UserRepository;
import app.service.CustomerService;
import app.service.InviteTokenService;
import app.service.LoginSecurityService;
import app.service.UserService;
import app.model.enums.TestPurchaseCategory;
import app.model.enums.TestPurchaseStatus;
import app.model.enums.TestPurchaseType;
import app.repository.TestPurchaseRepository;
import app.service.ShopService;
import app.service.TestPurchaseService;
import app.service.AttachmentService;
import app.model.enums.AttachmentCategory;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditLogIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingCustomerClient pricingCustomerClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private InviteTokenRepository inviteTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private LoginSecurityService loginSecurityService;

    @Autowired
    private InviteTokenService inviteTokenService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TestPurchaseService testPurchaseService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private TestPurchaseTestDataCleaner testPurchaseTestDataCleaner;

    private User admin;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            auditLogRepository.deleteAll();
            inviteTokenRepository.deleteAll();
        });
        testPurchaseTestDataCleaner.resetPurchaseGraphAndBusinessFixtures(
                customerRepository, userRepository, shopRepository);
        admin = transactionTemplate.execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            User u = User.builder()
                    .username("audit_admin_" + suffix)
                    .password(passwordEncoder.encode("AdminPass123!"))
                    .email("audit-admin-" + suffix + "@example.com")
                    .role(Role.ADMIN)
                    .country(Country.GERMANY)
                    .active(true)
                    .build();
            return userRepository.save(u);
        });
    }

    @Test
    void adminCanViewAuditLog_nonAdminGets403() throws Exception {
        mockMvc.perform(get("/audit").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk());

        User accountManager = transactionTemplate.execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            return userRepository.save(User.builder()
                    .username("audit_am_" + suffix)
                    .password(passwordEncoder.encode("AmPass123!"))
                    .email("audit-am-" + suffix + "@example.com")
                    .role(Role.ACCOUNT_MANAGER)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
        });

        mockMvc.perform(get("/audit").with(user(accountManager.getUsername()).roles("ACCOUNT_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCreate_doesNotStorePasswordInAuditSummary() {
        runAsAdmin(() -> {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("audit_user_" + UUID.randomUUID().toString().substring(0, 6));
            dto.setPassword("VerySecretPassword123!");
            dto.setConfirmPassword("VerySecretPassword123!");
            dto.setEmail("audit-user@example.com");
            dto.setRole(Role.MYSTERY_SHOPPER);
            dto.setCountry(Country.GERMANY);
            dto.setActive(true);
            userService.createUser(dto);
        });

        var entry = auditLogRepository.findAll().stream()
                .filter(a -> a.getAction() == AuditAction.USER_CREATE)
                .findFirst()
                .orElseThrow();
        assertFalse(entry.getChangeSummary().contains("VerySecretPassword123!"));
        assertFalse(entry.getChangeSummary().toLowerCase().contains("password="));
    }

    @Test
    void recordsSecurityAndDomainEvents() {
        User target = transactionTemplate.execute(status -> {
            User u = User.builder()
                    .username("login_target_" + UUID.randomUUID().toString().substring(0, 6))
                    .password(passwordEncoder.encode("TargetPass123!"))
                    .email("target@example.com")
                    .role(Role.MYSTERY_SHOPPER)
                    .country(Country.GERMANY)
                    .active(true)
                    .build();
            return userRepository.save(u);
        });

        AuditContext.setClientIp("203.0.113.55");
        try {
            loginSecurityService.handleFailedLogin("203.0.113.55", target.getUsername());
            loginSecurityService.handleSuccessfulLogin(target.getUsername());
        } finally {
            AuditContext.clear();
        }

        runAsAdmin(() -> {
            inviteTokenService.createInviteToken();

            ShopDTO shopDto = new ShopDTO();
            shopDto.setName("Audit Shop");
            shopDto.setCountry(Country.GERMANY);
            Shop shop = shopService.create(shopDto);
            shopDto.setName("Audit Shop Updated");
            shopService.update(shop.getId(), shopDto);
        });

        User salesManager = transactionTemplate.execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            return userRepository.save(User.builder()
                    .username("audit_sm_" + suffix)
                    .password(passwordEncoder.encode("SmPass123!"))
                    .email("audit-sm-" + suffix + "@example.com")
                    .role(Role.SALES_MANAGER)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
        });

        runAs(salesManager, Role.SALES_MANAGER, () -> {
            CustomerDTO customerDto = new CustomerDTO();
            customerDto.setName("Audit Customer");
            customerDto.setCategory("S");
            customerDto.setCountry(Country.GERMANY);
            customerDto.setEmail("cust-" + UUID.randomUUID().toString().substring(0, 6) + "@example.com");
            customerDto.setBaseServiceFee(10.0);
            Customer created = customerService.create(customerDto);
            customerDto.setName("Audit Customer Updated");
            customerService.update(created.getId(), customerDto);
            customerService.deactivateCustomer(created.getId());
        });

        List<AuditAction> actions = auditLogRepository.findAll().stream()
                .map(a -> a.getAction())
                .distinct()
                .toList();

        assertTrue(actions.contains(AuditAction.LOGIN_FAILURE));
        assertTrue(actions.contains(AuditAction.LOGIN_SUCCESS));
        assertTrue(actions.contains(AuditAction.INVITE_CREATE));
        assertTrue(actions.contains(AuditAction.CUSTOMER_CREATE));
        assertTrue(actions.contains(AuditAction.CUSTOMER_UPDATE));
        assertTrue(actions.contains(AuditAction.CUSTOMER_DEACTIVATE));
        assertTrue(actions.contains(AuditAction.SHOP_CREATE));
        assertTrue(actions.contains(AuditAction.SHOP_UPDATE));
    }

    @Test
    void recordsUserLifecycle_testPurchaseStatus_andFileUpload() {
        org.mockito.Mockito.when(pricingClient.calculatePrice(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new app.model.dto.PriceCalculationResponseDTO());

        User target = transactionTemplate.execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            return userRepository.save(User.builder()
                    .username("lifecycle_" + suffix)
                    .password(passwordEncoder.encode("TargetPass123!"))
                    .email("life-" + suffix + "@example.com")
                    .role(Role.MYSTERY_SHOPPER)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
        });

        runAsAdmin(() -> {
            UserEditDTO edit = new UserEditDTO();
            edit.setEmail(target.getEmail());
            edit.setCountry(target.getCountry());
            edit.setRole(Role.SALES_MANAGER);
            edit.setActive(true);
            userService.updateUserFromDto(target.getId(), edit);
            userService.deactivateUser(target.getId());
            userService.reactivateUser(target.getId());
        });

        Customer customer = transactionTemplate.execute(status -> customerRepository.save(Customer.builder()
                .name("TP Customer")
                .number("0099")
                .country(Country.GERMANY)
                .active(true)
                .build()));
        Shop shop = transactionTemplate.execute(status -> shopRepository.save(Shop.builder()
                .name("TP Shop")
                .number("0099")
                .country(Country.GERMANY)
                .build()));

        runAsAdmin(() -> {
            TestPurchaseCreateDTO tpDto = new TestPurchaseCreateDTO();
            tpDto.setCustomerId(customer.getId());
            tpDto.setShopId(shop.getId());
            tpDto.setCountry(Country.GERMANY);
            tpDto.setCategory(TestPurchaseCategory.S);
            tpDto.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
            app.model.dto.ItemDTO item = new app.model.dto.ItemDTO();
            item.setProductName("Demo");
            item.setQuantity(1);
            item.setUnitPrice(9.99);
            tpDto.setItems(List.of(item));
            var tp = testPurchaseService.create(tpDto);
            testPurchaseService.changeStatus(tp.getId(), TestPurchaseStatus.IN_PROGRESS, "demo");

            attachmentService.upload(
                    tp.getId(),
                    new org.springframework.mock.web.MockMultipartFile(
                            "file", "demo.pdf", "application/pdf", new byte[]{1, 2, 3}),
                    AttachmentCategory.INVOICE);
        });

        List<AuditAction> actions = auditLogRepository.findAll().stream().map(a -> a.getAction()).toList();
        assertTrue(actions.contains(AuditAction.USER_ROLE_CHANGE));
        assertTrue(actions.contains(AuditAction.USER_DEACTIVATE));
        assertTrue(actions.contains(AuditAction.USER_REACTIVATE));
        assertTrue(actions.contains(AuditAction.TEST_PURCHASE_CREATE));
        assertTrue(actions.contains(AuditAction.TEST_PURCHASE_STATUS_CHANGE));
        assertTrue(actions.contains(AuditAction.FILE_UPLOAD));
    }

    @Test
    void accountLockout_isAudited() {
        User target = transactionTemplate.execute(status -> {
            User u = User.builder()
                    .username("lock_" + UUID.randomUUID().toString().substring(0, 6))
                    .password(passwordEncoder.encode("TargetPass123!"))
                    .email("lock@example.com")
                    .role(Role.MYSTERY_SHOPPER)
                    .country(Country.GERMANY)
                    .active(true)
                    .build();
            return userRepository.save(u);
        });

        for (int i = 0; i < 3; i++) {
            loginSecurityService.handleFailedLogin("203.0.113.9", target.getUsername());
        }

        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(a -> a.getAction() == AuditAction.ACCOUNT_LOCKOUT));
    }

    private void runAsAdmin(Runnable action) {
        runAs(admin, Role.ADMIN, action);
    }

    private void runAs(User user, Role role, Runnable action) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
        AuditContext.setClientIp("127.0.0.1");
        try {
            transactionTemplate.executeWithoutResult(status -> action.run());
        } finally {
            AuditContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
