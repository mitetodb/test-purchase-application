package app.security;

import app.TestPurchaseApplication;
import app.model.dto.QuestionnaireResultViewDTO;
import app.model.dto.QuestionnaireSubmitDTO;
import app.model.entity.*;
import app.model.enums.*;
import app.repository.*;
import app.service.AccessControlService;
import app.service.QuestionnaireService;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class QuestionnaireSecurityTest {

    @Autowired
    private QuestionnaireService questionnaireService;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private QuestionnaireTemplateRepository templateRepository;

    @Autowired
    private QuestionnaireQuestionRepository questionRepository;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestPurchaseTestDataCleaner testPurchaseTestDataCleaner;

    private TestPurchase purchase;
    private User mysteryShopper;
    private User customerUser;
    private Map<UUID, Integer> defaultScores;

    @BeforeEach
    void setUp() {
        testPurchaseTestDataCleaner.resetPurchaseGraphAndBusinessFixtures(
                customerRepository, userRepository, shopRepository);

        Customer customer = customerRepository.save(buildCustomer("Acme", null));
        Customer other = customerRepository.save(buildCustomer("Other Co", null));
        Shop shop = shopRepository.save(buildShop());

        mysteryShopper = userRepository.save(buildUser("ms_q", Role.MYSTERY_SHOPPER, null));
        customerUser = userRepository.save(buildUser("cust_q", Role.CUSTOMER, customer));
        userRepository.save(buildUser("other_cust", Role.CUSTOMER, other));
        userRepository.save(buildUser("admin_q", Role.ADMIN, null));

        purchase = buildPurchase(customer, shop, mysteryShopper);
        purchase = testPurchaseRepository.save(purchase);

        UUID templateId = templateRepository.findByCodeAndActiveTrue("DEFAULT").orElseThrow().getId();
        defaultScores = new LinkedHashMap<>();
        for (QuestionnaireQuestion q : questionRepository.findActiveByTemplateId(templateId)) {
            defaultScores.put(q.getId(), 4);
        }
    }

    @Test
    void mysteryShopperAssigned_canSubmitQuestionnaire() {
        authenticate("ms_q");
        QuestionnaireSubmitDTO dto = new QuestionnaireSubmitDTO();
        dto.setScores(defaultScores);
        questionnaireService.submitQuestionnaire(purchase.getId(), dto);
        assertTrue(questionnaireService.hasResponse(purchase.getId()));
    }

    @Test
    void customer_cannotSubmitQuestionnaire() {
        authenticate("cust_q");
        QuestionnaireSubmitDTO dto = new QuestionnaireSubmitDTO();
        dto.setScores(defaultScores);
        assertThrows(AccessDeniedException.class,
                () -> questionnaireService.submitQuestionnaire(purchase.getId(), dto));
    }

    @Test
    void customer_seesResultsWithoutEvaluatorIdentity() {
        authenticate("ms_q");
        QuestionnaireSubmitDTO dto = new QuestionnaireSubmitDTO();
        dto.setScores(defaultScores);
        questionnaireService.submitQuestionnaire(purchase.getId(), dto);

        authenticate("cust_q");
        QuestionnaireResultViewDTO result = questionnaireService.getResultForTestPurchase(purchase.getId());
        assertTrue(result.isHideEvaluatorIdentity());
        assertNull(result.getEvaluatorDisplayName());
        assertNotNull(result.getOverallScore());
    }

    @Test
    void admin_seesEvaluatorIdentityOnResults() {
        authenticate("ms_q");
        QuestionnaireSubmitDTO dto = new QuestionnaireSubmitDTO();
        dto.setScores(defaultScores);
        questionnaireService.submitQuestionnaire(purchase.getId(), dto);

        authenticate("admin_q");
        QuestionnaireResultViewDTO result = questionnaireService.getResultForTestPurchase(purchase.getId());
        assertFalse(result.isHideEvaluatorIdentity());
        assertEquals("ms_q", result.getEvaluatorDisplayName());
    }

    @Test
    void mysteryShopper_cannotViewScoreTrends() {
        authenticate("ms_q");
        assertFalse(accessControlService.canViewScoreTrends());
        assertThrows(AccessDeniedException.class, () -> questionnaireService.getScoreTrendsByMonthForCurrentUser());
    }

    private void authenticate(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }

    private static Customer buildCustomer(String name, User salesManager) {
        Customer c = new Customer();
        c.setName(name);
        c.setNumber(String.valueOf(System.nanoTime()).substring(8));
        c.setCountry(Country.GERMANY);
        c.setCategory("GOLD");
        c.setActive(true);
        c.setSalesManager(salesManager);
        return c;
    }

    private static Shop buildShop() {
        Shop shop = new Shop();
        shop.setName("Shop Q");
        shop.setNumber(String.valueOf(System.nanoTime()).substring(7));
        shop.setCountry(Country.GERMANY);
        shop.setActive(true);
        return shop;
    }

    private static User buildUser(String username, Role role, Customer customer) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setEmail(username + "@test.com");
        user.setRole(role);
        user.setCustomer(customer);
        user.setActive(true);
        return user;
    }

    private static TestPurchase buildPurchase(Customer customer, Shop shop, User mysteryShopper) {
        TestPurchase tp = new TestPurchase();
        tp.setNumber("TP-" + System.nanoTime());
        tp.setCustomer(customer);
        tp.setShop(shop);
        tp.setMysteryShopper(mysteryShopper);
        tp.setCountry(Country.GERMANY);
        tp.setCategory(TestPurchaseCategory.S);
        tp.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
        tp.setStatus(TestPurchaseStatus.IN_PROGRESS);
        tp.setActive(true);
        tp.setProductPrice(100.0);
        tp.setServiceFee(10.0);
        tp.setPostageFee(5.0);
        return tp;
    }
}
