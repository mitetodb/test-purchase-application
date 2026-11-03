package app.security;

import app.TestPurchaseApplication;
import app.model.entity.Attachment;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.AttachmentCategory;
import app.model.enums.AttachmentFileType;
import app.model.enums.Country;
import app.model.enums.Role;
import app.model.enums.TestPurchaseCategory;
import app.model.enums.TestPurchaseStatus;
import app.model.enums.TestPurchaseType;
import app.repository.AttachmentRepository;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.TestPurchaseStatusHistoryRepository;
import app.repository.UserRepository;
import app.service.TestPurchaseService;
import app.support.TestActiveUsers;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TestPurchaseArchiveMvcIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingCustomerClient pricingCustomerClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private TestPurchaseStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private TestPurchaseService testPurchaseService;

    @Autowired
    private TestPurchaseTestDataCleaner testPurchaseTestDataCleaner;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final List<UUID> createdUserIds = new ArrayList<>();
    private final List<UUID> createdCustomerIds = new ArrayList<>();
    private final List<UUID> createdShopIds = new ArrayList<>();
    private final List<UUID> createdPurchaseIds = new ArrayList<>();
    private final List<UUID> createdAttachmentIds = new ArrayList<>();

    private String suffix;
    private User admin;
    private TestPurchase targetPurchase;
    private Attachment attachment;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        TestActiveUsers.seedStandardUsers(userRepository, passwordEncoder);
        transactionTemplate.executeWithoutResult(status -> {
            admin = userRepository.save(User.builder()
                    .username("tp_arch_admin_" + suffix)
                    .email("tp_arch_admin_" + suffix + "@arch.test")
                    .password(passwordEncoder.encode("AdminPass1!"))
                    .role(Role.ADMIN)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(admin.getId());

            Customer customer = customerRepository.save(Customer.builder()
                    .number("CA" + suffix)
                    .name("Customer " + suffix)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdCustomerIds.add(customer.getId());

            Shop shop = shopRepository.save(Shop.builder()
                    .number("SA" + suffix)
                    .name("Shop " + suffix)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdShopIds.add(shop.getId());

            targetPurchase = testPurchaseRepository.save(TestPurchase.builder()
                    .number("TP-AR-" + suffix)
                    .customer(customer)
                    .shop(shop)
                    .country(Country.GERMANY)
                    .category(TestPurchaseCategory.M)
                    .type(TestPurchaseType.RETURN_BACK_TO_SELLER)
                    .status(TestPurchaseStatus.INITIALISED)
                    .productPrice(10.0)
                    .serviceFee(1.0)
                    .postageFee(1.0)
                    .active(true)
                    .build());
            createdPurchaseIds.add(targetPurchase.getId());
            targetPurchase.setStatus(TestPurchaseStatus.IN_PROGRESS);
            targetPurchase = testPurchaseRepository.save(targetPurchase);

            attachment = attachmentRepository.save(Attachment.builder()
                    .testPurchase(targetPurchase)
                    .name("proof.pdf")
                    .category(AttachmentCategory.INVOICE)
                    .fileType(AttachmentFileType.PDF)
                    .filePath("/tmp/" + suffix + "-proof.pdf")
                    .fileSize(100L)
                    .build());
            createdAttachmentIds.add(attachment.getId());
        });
    }

    @AfterEach
    void tearDown() {
        TestActiveUsers.deleteStandardUsers(userRepository);
        testPurchaseTestDataCleaner.deletePurchasesByIds(createdPurchaseIds);
        transactionTemplate.executeWithoutResult(status -> {
            createdUserIds.forEach(userRepository::deleteById);
            createdCustomerIds.forEach(customerRepository::deleteById);
            createdShopIds.forEach(shopRepository::deleteById);
            createdAttachmentIds.clear();
            createdPurchaseIds.clear();
            createdUserIds.clear();
            createdCustomerIds.clear();
            createdShopIds.clear();
        });
    }

    @Test
    void adminArchiveKeepsRowAttachmentsAndHistory() throws Exception {
        mockMvc.perform(post("/testpurchases/archive/" + targetPurchase.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/testpurchases"));

        TestPurchase refreshed = testPurchaseRepository.findById(targetPurchase.getId()).orElseThrow();
        assertTrue(refreshed.getStatus().isArchived());
        assertNotNull(attachmentRepository.findById(attachment.getId()).orElse(null));
        assertTrue(statusHistoryRepository.findByTestPurchaseIdOrderByChangedAtDesc(targetPurchase.getId()).stream()
                .anyMatch(h -> h.getNewStatus().isArchived()));
    }

    @Test
    void listHidesArchivedByDefault() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getUsername(),
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            testPurchaseService.archive(targetPurchase.getId());
        } finally {
            SecurityContextHolder.clearContext();
        }

        mockMvc.perform(get("/testpurchases")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(not(containsString(targetPurchase.getNumber()))));
    }

    @Test
    void adminFilterShowsArchivedPurchases() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getUsername(),
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            testPurchaseService.archive(targetPurchase.getId());
        } finally {
            SecurityContextHolder.clearContext();
        }

        mockMvc.perform(get("/testpurchases")
                        .param("showArchived", "true")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString(targetPurchase.getNumber())));
    }

    @Test
    void deleteEndpointDoesNotRemovePurchase() throws Exception {
        mockMvc.perform(post("/testpurchases/delete/" + targetPurchase.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        assertNotNull(testPurchaseRepository.findById(targetPurchase.getId()).orElse(null));
    }

    @Test
    void attachmentDeleteEndpointIsGone() throws Exception {
        mockMvc.perform(post("/attachments/delete/" + attachment.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        assertNotNull(attachmentRepository.findById(attachment.getId()).orElse(null));
    }

    @Test
    void archivedPurchaseIsReadOnlyInService() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getUsername(),
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            testPurchaseService.archive(targetPurchase.getId());
            assertThrows(AccessDeniedException.class, () ->
                    testPurchaseService.changeStatus(targetPurchase.getId(), TestPurchaseStatus.CLOSED, null));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void adminCanUnarchivePurchase() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getUsername(),
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            testPurchaseService.archive(targetPurchase.getId());
        } finally {
            SecurityContextHolder.clearContext();
        }

        mockMvc.perform(post("/testpurchases/unarchive/" + targetPurchase.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/testpurchases/view/" + targetPurchase.getId()));

        assertEquals(TestPurchaseStatus.IN_PROGRESS,
                testPurchaseRepository.findById(targetPurchase.getId()).orElseThrow().getStatus());
    }
}
