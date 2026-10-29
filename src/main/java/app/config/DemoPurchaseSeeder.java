package app.config;

import app.model.dto.CustomerDTO;
import app.model.dto.ItemDTO;
import app.model.dto.ShopDTO;
import app.model.dto.TestPurchaseCreateDTO;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.TestPurchaseStatusHistory;
import app.model.enums.Country;
import app.model.enums.TestPurchaseCategory;
import app.model.enums.TestPurchaseStatus;
import app.model.enums.TestPurchaseType;
import app.repository.CustomerRepository;
import app.repository.TestPurchaseRepository;
import app.repository.TestPurchaseStatusHistoryRepository;
import app.repository.UserRepository;
import app.service.CustomerService;
import app.service.ShopService;
import app.service.TestPurchaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Opt-in demo seed (app.demo-data.enabled=true / env DEMO_DATA_ENABLED, plus app.demo-data.purchases > 0):
 * fictional demo customers (Samsung, LG, Apple with example.com contacts), shops in DE/AT and N test purchases
 * for phones, tablets and TVs spread across every status, with consistent, back-dated status history.
 * Prices/fees are calculated by the pricing service through TestPurchaseService.create().
 * Status history is written directly (no status e-mails are sent). Idempotent: skipped if the demo customers exist.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoPurchaseSeeder implements CommandLineRunner {

    static final String MARKER_EMAIL = "demo.samsung@example.com";

    private final CustomerService customerService;
    private final ShopService shopService;
    private final TestPurchaseService testPurchaseService;
    private final CustomerRepository customerRepository;
    private final TestPurchaseRepository testPurchaseRepository;
    private final TestPurchaseStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final EntityManager em;
    private final TransactionTemplate tx;

    @Value("${app.demo-data.purchases:0}")
    private int purchaseCount;

    record Product(String brand, String name, double price, TestPurchaseCategory category, String slug) {}

    static final List<Product> PRODUCTS = List.of(
            new Product("Samsung", "Samsung Galaxy S24 128GB", 899.00, TestPurchaseCategory.S, "galaxy-s24"),
            new Product("Samsung", "Samsung Galaxy S24 Ultra 256GB", 1449.00, TestPurchaseCategory.S, "galaxy-s24-ultra"),
            new Product("Samsung", "Samsung Galaxy A55 5G 128GB", 479.00, TestPurchaseCategory.S, "galaxy-a55"),
            new Product("Samsung", "Samsung Galaxy Tab S9 128GB WiFi", 899.00, TestPurchaseCategory.M, "galaxy-tab-s9"),
            new Product("Samsung", "Samsung Galaxy Tab A9+ 64GB", 269.00, TestPurchaseCategory.M, "galaxy-tab-a9"),
            new Product("Samsung", "Samsung 55\" Neo QLED QN90D", 1599.00, TestPurchaseCategory.XL, "qn90d-55"),
            new Product("Samsung", "Samsung 65\" Crystal UHD DU8079", 749.00, TestPurchaseCategory.XL, "du8079-65"),
            new Product("LG", "LG OLED55C47LA 55\"", 1399.00, TestPurchaseCategory.XL, "oled55c4"),
            new Product("LG", "LG OLED65G48LW 65\"", 2999.00, TestPurchaseCategory.XL, "oled65g4"),
            new Product("LG", "LG 43UR78006LK 43\" UHD", 349.00, TestPurchaseCategory.L, "43ur78"),
            new Product("LG", "LG QNED 50QNED82T 50\"", 649.00, TestPurchaseCategory.L, "50qned82"),
            new Product("LG", "LG StanbyME 27ART10AKPL", 999.00, TestPurchaseCategory.L, "stanbyme"),
            new Product("Apple", "Apple iPhone 15 128GB", 849.00, TestPurchaseCategory.S, "iphone-15"),
            new Product("Apple", "Apple iPhone 15 Pro 256GB", 1329.00, TestPurchaseCategory.S, "iphone-15-pro"),
            new Product("Apple", "Apple iPhone 16 128GB", 949.00, TestPurchaseCategory.S, "iphone-16"),
            new Product("Apple", "Apple iPad Air 11\" M2 128GB", 699.00, TestPurchaseCategory.M, "ipad-air-m2"),
            new Product("Apple", "Apple iPad Pro 13\" M4 256GB", 1549.00, TestPurchaseCategory.M, "ipad-pro-m4"),
            new Product("Apple", "Apple iPad 10.9\" 64GB (10th gen)", 399.00, TestPurchaseCategory.M, "ipad-10"));

    static final TestPurchaseStatus[] TARGETS = TestPurchaseStatus.values();

    @Override
    public void run(String... args) {
        if (purchaseCount <= 0) return;
        if (customerRepository.findAll().stream().anyMatch(c -> MARKER_EMAIL.equalsIgnoreCase(c.getEmail()))) {
            log.info("Demo purchases already seeded - skipping.");
            return;
        }
        var admin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == app.model.enums.Role.ADMIN).findFirst().orElse(null);
        if (admin == null) {
            log.warn("Demo seed skipped: admin user does not exist yet.");
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                admin.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            seed();
        } catch (Exception e) {
            log.warn("Demo seed aborted: {}", e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    void seed() {
        Customer samsung = customer("Samsung", "demo.samsung@example.com", Country.GERMANY, 24.0);
        Customer lg = customer("LG", "demo.lg@example.com", Country.GERMANY, 22.0);
        Customer apple = customer("Apple", "demo.apple@example.com", Country.AUSTRIA, 26.0);

        List<Shop> shops = List.of(
                shop("ElektroBlitz Berlin (Demo)", Country.GERMANY),
                shop("Technikwelt München (Demo)", Country.GERMANY),
                shop("MegaMedia Hamburg (Demo)", Country.GERMANY),
                shop("Donau Elektronik Wien (Demo)", Country.AUSTRIA),
                shop("Alpen Tech Graz (Demo)", Country.AUSTRIA));

        Random rnd = new Random(20261029L);
        LocalDateTime now = LocalDateTime.now().withNano(0);

        for (int i = 0; i < purchaseCount; i++) {
            TestPurchaseStatus target = TARGETS[i % TARGETS.length];
            TestPurchaseType type = switch (target) {
                case PRODUCT_FORWARDED_TO_CLIENT -> TestPurchaseType.FORWARDING_TO_CLIENT;
                case PRODUCT_SHIPPED_BACK_TO_SELLER -> TestPurchaseType.RETURN_BACK_TO_SELLER;
                default -> rnd.nextBoolean() ? TestPurchaseType.FORWARDING_TO_CLIENT : TestPurchaseType.RETURN_BACK_TO_SELLER;
            };
            Customer customer = switch (i % 3) { case 0 -> samsung; case 1 -> lg; default -> apple; };
            List<Product> brandProducts = PRODUCTS.stream().filter(p -> p.brand().equals(customer.getName())).toList();
            Product product = brandProducts.get(rnd.nextInt(brandProducts.size()));
            Shop shop = shops.get(rnd.nextInt(shops.size()));

            TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
            dto.setCustomerId(customer.getId());
            dto.setShopId(shop.getId());
            dto.setCountry(shop.getCountry());
            dto.setCategory(product.category());
            dto.setType(type);
            ItemDTO item = new ItemDTO();
            item.setProductName(product.name());
            item.setProductUrl("https://shop.example.com/p/" + product.slug());
            item.setQuantity(1);
            item.setUnitPrice(product.price());
            dto.setItems(new ArrayList<>(List.of(item)));

            TestPurchase tp = testPurchaseService.create(dto);   // pricing service calculates fees

            List<TestPurchaseStatus> path = pathTo(target, type);
            LocalDateTime created = now.minusDays(10 + rnd.nextInt(170)).minusMinutes(rnd.nextInt(600));
            LocalDateTime t = created;
            TestPurchaseStatus prev = TestPurchaseStatus.INITIALISED;
            for (TestPurchaseStatus next : path) {
                t = t.plusDays(1 + rnd.nextInt(5)).plusMinutes(rnd.nextInt(300));
                if (t.isAfter(now)) t = now.minusMinutes(path.size());
                TestPurchaseStatusHistory h = new TestPurchaseStatusHistory();
                h.setTestPurchase(tp);
                h.setOldStatus(prev);
                h.setNewStatus(next);
                h.setChangedAt(t);
                h.setChangedBy(tp.getUpdatedByUser());
                h.setComment("Demo data");
                historyRepository.save(h);
                prev = next;
            }
            final LocalDateTime createdAt = created, updatedAt = path.isEmpty() ? null : t;
            tx.executeWithoutResult(st -> em.createQuery(
                    "update TestPurchase p set p.status = :s, p.createdOn = :c, p.updatedOn = :u where p.id = :id")
                    .setParameter("s", target).setParameter("c", createdAt).setParameter("u", updatedAt)
                    .setParameter("id", tp.getId()).executeUpdate());
        }
        log.info("Demo seed created {} test purchases for 3 demo customers and {} shops.", purchaseCount, shops.size());
    }

    static List<TestPurchaseStatus> pathTo(TestPurchaseStatus target, TestPurchaseType type) {
        TestPurchaseStatus handOver = type == TestPurchaseType.FORWARDING_TO_CLIENT
                ? TestPurchaseStatus.PRODUCT_FORWARDED_TO_CLIENT : TestPurchaseStatus.PRODUCT_SHIPPED_BACK_TO_SELLER;
        List<TestPurchaseStatus> full = List.of(TestPurchaseStatus.IN_PROGRESS, TestPurchaseStatus.PRODUCT_ORDERED,
                TestPurchaseStatus.PRODUCT_RECEIVED, handOver, TestPurchaseStatus.CLOSED);
        return switch (target) {
            case INITIALISED -> List.of();
            case CANCELLED -> List.of(TestPurchaseStatus.IN_PROGRESS, TestPurchaseStatus.CANCELLED);
            default -> full.subList(0, full.indexOf(target) + 1);
        };
    }

    private Customer customer(String name, String email, Country country, double fee) {
        CustomerDTO dto = new CustomerDTO();
        dto.setName(name);
        dto.setEmail(email);
        dto.setCountry(country);
        dto.setCategory("Demo");
        dto.setBaseServiceFee(fee);
        return customerService.create(dto);
    }

    private Shop shop(String name, Country country) {
        ShopDTO dto = new ShopDTO();
        dto.setName(name);
        dto.setCountry(country);
        dto.setDescription("Fictional demo shop");
        dto.setNotes("Demo data");
        return shopService.create(dto);
    }
}
