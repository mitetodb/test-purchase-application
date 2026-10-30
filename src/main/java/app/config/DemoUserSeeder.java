package app.config;

import app.model.entity.Customer;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.CustomerRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Opt-in demo users (app.demo-data.enabled=true and app.demo-data.users=true / env DEMO_USERS):
 * one or two fictional users per non-admin role (example.com e-mails). Account managers get the demo customers
 * assigned, mystery shoppers get the not yet assigned demo test purchases. Existing users (by username or e-mail)
 * are never touched. Initial passwords are random and never logged; operators set real ones out of band.
 * Note: the CUSTOMER role has no link to a Customer entity in the data model, so customer users are only
 * associated by name/e-mail.
 */
@Slf4j
@Component
@Order(110)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoUserSeeder implements CommandLineRunner {

    record DemoUser(String username, String email, Role role, Country country, List<String> customers) {}

    static final Set<String> DEMO_CUSTOMER_EMAILS =
            Set.of("demo.samsung@example.com", "demo.lg@example.com", "demo.apple@example.com");

    static final List<DemoUser> USERS = List.of(
            new DemoUser("demo.am.anna", "anna.schneider@example.com", Role.ACCOUNT_MANAGER, Country.GERMANY,
                    List.of("demo.samsung@example.com", "demo.lg@example.com")),
            new DemoUser("demo.am.lukas", "lukas.huber@example.com", Role.ACCOUNT_MANAGER, Country.AUSTRIA,
                    List.of("demo.apple@example.com")),
            new DemoUser("demo.sm.julia", "julia.weber@example.com", Role.SALES_MANAGER, Country.GERMANY, List.of()),
            new DemoUser("demo.sm.markus", "markus.leitner@example.com", Role.SALES_MANAGER, Country.AUSTRIA, List.of()),
            new DemoUser("demo.cust.samsung", "jisoo.park.samsung@example.com", Role.CUSTOMER, Country.GERMANY, List.of()),
            new DemoUser("demo.cust.lg", "minho.lee.lg@example.com", Role.CUSTOMER, Country.GERMANY, List.of()),
            new DemoUser("demo.cust.apple", "emily.carter.apple@example.com", Role.CUSTOMER, Country.AUSTRIA, List.of()),
            new DemoUser("demo.ms.tim", "tim.becker@example.com", Role.MYSTERY_SHOPPER, Country.GERMANY, List.of()),
            new DemoUser("demo.ms.lena", "lena.gruber@example.com", Role.MYSTERY_SHOPPER, Country.AUSTRIA, List.of()));

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final TestPurchaseRepository testPurchaseRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;

    @Value("${app.demo-data.users:false}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) return;
        try {
            tx.executeWithoutResult(s -> seed());
        } catch (Exception e) {
            log.warn("Demo user seed aborted: {}", e.getClass().getSimpleName());
        }
    }

    void seed() {
        var existing = userRepository.findAll();
        var random = new SecureRandom();
        List<User> shoppers = new ArrayList<>();
        int created = 0;
        for (DemoUser d : USERS) {
            boolean exists = existing.stream().anyMatch(u -> d.username().equalsIgnoreCase(u.getUsername())
                    || d.email().equalsIgnoreCase(u.getEmail()));
            if (exists) continue;
            byte[] secret = new byte[24];
            random.nextBytes(secret);
            User u = new User();
            u.setUsername(d.username());
            u.setEmail(d.email());
            u.setPassword(passwordEncoder.encode(Base64.getUrlEncoder().encodeToString(secret)));
            u.setRole(d.role());
            u.setCountry(d.country());
            u.setActive(true);
            u.setUpdatedByUser("demo-seed");
            if (d.role() == Role.ACCOUNT_MANAGER) {
                Set<Customer> managed = new HashSet<>();
                customerRepository.findAll().stream()
                        .filter(c -> c.getEmail() != null && d.customers().contains(c.getEmail().toLowerCase()))
                        .forEach(managed::add);
                u.setManagedCustomers(managed);
            }
            u = userRepository.save(u);
            if (d.role() == Role.MYSTERY_SHOPPER) shoppers.add(u);
            created++;
        }
        int assigned = 0;
        if (!shoppers.isEmpty()) {
            List<TestPurchase> open = testPurchaseRepository.findAll().stream()
                    .filter(p -> p.getMysteryShopper() == null && p.getCustomer() != null
                            && p.getCustomer().getEmail() != null
                            && DEMO_CUSTOMER_EMAILS.contains(p.getCustomer().getEmail().toLowerCase()))
                    .sorted(Comparator.comparing(TestPurchase::getCreatedOn,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            for (int i = 0; i < open.size(); i++) {
                TestPurchase p = open.get(i);
                // match shopper country to the customer's country, otherwise round-robin
                int idx = i;
                User s = shoppers.stream()
                        .filter(x -> Objects.equals(x.getCountry(), p.getCustomer().getCountry()))
                        .findFirst().orElseGet(() -> shoppers.get(idx % shoppers.size()));
                p.setMysteryShopper(s);
                testPurchaseRepository.save(p);
                assigned++;
            }
        }
        log.info("Demo user seed created {} users, assigned {} demo purchases to mystery shoppers.", created, assigned);
    }
}
