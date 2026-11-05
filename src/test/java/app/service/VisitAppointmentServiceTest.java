package app.service;

import app.TestPurchaseApplication;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.AuditAction;
import app.model.enums.Country;
import app.model.enums.Role;
import app.model.enums.TestPurchaseCategory;
import app.model.enums.TestPurchaseStatus;
import app.model.enums.TestPurchaseType;
import app.repository.AuditLogRepository;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.service.VisitAppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class VisitAppointmentServiceTest {

    @Autowired
    private VisitAppointmentService visitAppointmentService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User accountManager;
    private User mysteryShopper1;
    private User mysteryShopper2;
    private User customerUser;
    private Customer customer;
    private TestPurchase assignedPurchase;
    private TestPurchase otherPurchase;

    @BeforeEach
    void setUp() {
        accountManager = saveUser("am_visit", Role.ACCOUNT_MANAGER);
        mysteryShopper1 = saveUser("ms1_visit", Role.MYSTERY_SHOPPER);
        mysteryShopper2 = saveUser("ms2_visit", Role.MYSTERY_SHOPPER);
        customerUser = saveUser("cust_visit", Role.CUSTOMER);

        customer = Customer.builder()
                .number(String.format("%05d", (int) (Math.random() * 100000)))
                .name("Visit Demo Customer")
                .email("customer-visit@example.com")
                .country(Country.GERMANY)
                .active(true)
                .build();
        customer = customerRepository.save(customer);

        accountManager.getManagedCustomers().add(customer);
        userRepository.save(accountManager);

        customerUser.setCustomer(customer);
        userRepository.save(customerUser);

        Shop shop = Shop.builder()
                .number(String.format("%05d", (int) (Math.random() * 100000)))
                .name("Visit Shop")
                .country(Country.GERMANY)
                .active(true)
                .build();
        shop = shopRepository.save(shop);

        assignedPurchase = buildPurchase(customer, shop, mysteryShopper1, TestPurchaseStatus.IN_PROGRESS);
        otherPurchase = buildPurchase(customer, shop, mysteryShopper2, TestPurchaseStatus.IN_PROGRESS);
    }

    @Test
    void accountManagerSchedulesConfirmedVisit() {
        runAs(accountManager, () -> {
            LocalDateTime when = LocalDateTime.now().plusDays(3).withNano(0);
            visitAppointmentService.scheduleOrUpdate(
                    assignedPurchase.getId(), when, "Main store", "Bring ID", false);

            TestPurchase saved = testPurchaseRepository.findById(assignedPurchase.getId()).orElseThrow();
            assertThat(saved.getVisitAppointmentAt()).isEqualTo(when);
            assertThat(saved.getVisitAppointmentLocation()).isEqualTo("Main store");
            assertThat(saved.isVisitAppointmentConfirmed()).isTrue();
        });

        assertThat(auditLogRepository.findAll().stream()
                .filter(log -> assignedPurchase.getId().toString().equals(log.getEntityId()))
                .map(log -> log.getAction()))
                .contains(AuditAction.APPOINTMENT_SCHEDULE);
    }

    @Test
    void mysteryShopperProposesThenConfirms() {
        runAs(mysteryShopper1, () -> {
            LocalDateTime when = LocalDateTime.now().plusDays(2).withNano(0);
            visitAppointmentService.scheduleOrUpdate(
                    assignedPurchase.getId(), when, null, "Proposed slot", false);

            TestPurchase saved = testPurchaseRepository.findById(assignedPurchase.getId()).orElseThrow();
            assertThat(saved.isVisitAppointmentConfirmed()).isFalse();

            visitAppointmentService.confirm(assignedPurchase.getId());
            assertThat(testPurchaseRepository.findById(assignedPurchase.getId()).orElseThrow()
                    .isVisitAppointmentConfirmed()).isTrue();
        });

        assertThat(auditLogRepository.findAll().stream()
                .filter(log -> assignedPurchase.getId().toString().equals(log.getEntityId()))
                .map(log -> log.getAction()))
                .contains(AuditAction.APPOINTMENT_CONFIRM);
    }

    @Test
    void mysteryShopperCannotScheduleForOtherAssignment() {
        runAs(mysteryShopper1, () -> {
            LocalDateTime when = LocalDateTime.now().plusDays(1).withNano(0);
            assertThrows(AccessDeniedException.class, () ->
                    visitAppointmentService.scheduleOrUpdate(
                            otherPurchase.getId(), when, null, null, true));
        });
    }

    @Test
    void rejectsPastTimeAndCancelledPurchase() {
        runAs(accountManager, () -> {
            assertThrows(IllegalArgumentException.class, () ->
                    visitAppointmentService.scheduleOrUpdate(
                            assignedPurchase.getId(),
                            LocalDateTime.now().minusHours(1),
                            null,
                            null,
                            true));

            assignedPurchase.setStatus(TestPurchaseStatus.CANCELLED);
            testPurchaseRepository.save(assignedPurchase);

            assertThrows(IllegalArgumentException.class, () ->
                    visitAppointmentService.scheduleOrUpdate(
                            assignedPurchase.getId(),
                            LocalDateTime.now().plusDays(1),
                            null,
                            null,
                            true));
        });
    }

    @Test
    void archivedPurchaseIsReadOnly() {
        assignedPurchase.setStatus(TestPurchaseStatus.ARCHIVED);
        testPurchaseRepository.save(assignedPurchase);

        runAs(accountManager, () -> assertThrows(AccessDeniedException.class, () ->
                visitAppointmentService.scheduleOrUpdate(
                        assignedPurchase.getId(),
                        LocalDateTime.now().plusDays(1),
                        null,
                        null,
                        true)));
    }

    private TestPurchase buildPurchase(Customer cust, Shop shop, User shopper, TestPurchaseStatus status) {
        TestPurchase tp = new TestPurchase();
        tp.setCustomer(cust);
        tp.setShop(shop);
        tp.setMysteryShopper(shopper);
        tp.setCountry(Country.GERMANY);
        tp.setCategory(TestPurchaseCategory.S);
        tp.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
        tp.setStatus(status);
        tp.setActive(true);
        tp.setNumber("TP-VIS-" + System.nanoTime());
        tp.setProductPrice(100.0);
        tp.setServiceFee(10.0);
        tp.setPostageFee(5.0);
        return testPurchaseRepository.save(tp);
    }

    private User saveUser(String username, Role role) {
        User u = User.builder()
                .username(username)
                .email(username + "@example.com")
                .password(passwordEncoder.encode("password"))
                .role(role)
                .active(true)
                .country(Country.GERMANY)
                .managedCustomers(new HashSet<>())
                .build();
        return userRepository.save(u);
    }

    private void runAs(User user, Runnable action) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
