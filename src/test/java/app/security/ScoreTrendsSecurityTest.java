package app.security;

import app.TestPurchaseApplication;
import app.model.dto.QuestionnaireSubmitDTO;
import app.model.dto.ScoreTrendMonthDTO;
import app.model.entity.*;
import app.model.enums.*;
import app.repository.*;
import app.service.QuestionnaireService;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScoreTrendsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestionnaireService questionnaireService;

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

    @MockBean
    private app.client.PricingClient pricingClient;

    private Map<UUID, Integer> defaultScores;

    @BeforeEach
    void setUp() {
        testPurchaseTestDataCleaner.resetPurchaseGraphAndBusinessFixtures(
                customerRepository, userRepository, shopRepository);

        Customer scoped = saveCustomer("Scoped");
        Customer other = saveCustomer("Out of scope");
        Shop shop = saveShop("Trend Shop");

        User admin = saveUser("trend_admin", Role.ADMIN, null);
        saveUser("scoped_cust", Role.CUSTOMER, scoped);
        saveUser("other_cust_user", Role.CUSTOMER, other);

        TestPurchase inScope = savePurchase(scoped, shop, null);
        TestPurchase outScope = savePurchase(other, shop, null);

        UUID templateId = templateRepository.findByCodeAndActiveTrue("DEFAULT").orElseThrow().getId();
        defaultScores = new LinkedHashMap<>();
        for (QuestionnaireQuestion q : questionRepository.findActiveByTemplateId(templateId)) {
            defaultScores.put(q.getId(), 5);
        }

        authenticate(admin);
        submit(inScope.getId());
        submit(outScope.getId());
    }

    @Test
    void customerTrends_onlyIncludeOwnCompanyResponses() {
        authenticate(userRepository.findByUsername("scoped_cust").orElseThrow());
        List<ScoreTrendMonthDTO> trends = questionnaireService.getScoreTrendsByMonthForCurrentUser();
        assertEquals(1, trends.size());
        assertEquals(1, trends.get(0).getResponseCount());
    }

    @Test
    void customer_cannotOpenQuestionnaireForForeignPurchase() throws Exception {
        TestPurchase foreign = testPurchaseRepository.findAll().stream()
                .filter(p -> "Out of scope".equals(p.getCustomer().getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/testpurchases/" + foreign.getId() + "/questionnaire")
                        .with(user("scoped_cust").roles("CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customer_canOpenScoreTrendsPage() throws Exception {
        mockMvc.perform(get("/score-trends")
                        .with(user("scoped_cust").roles("CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    private void submit(UUID purchaseId) {
        QuestionnaireSubmitDTO dto = new QuestionnaireSubmitDTO();
        dto.setScores(defaultScores);
        questionnaireService.submitQuestionnaire(purchaseId, dto);
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }

    private Customer saveCustomer(String name) {
        Customer c = new Customer();
        c.setName(name);
        c.setNumber(String.valueOf(System.nanoTime()).substring(8));
        c.setCountry(Country.GERMANY);
        c.setCategory("GOLD");
        c.setActive(true);
        return customerRepository.save(c);
    }

    private Shop saveShop(String name) {
        Shop shop = new Shop();
        shop.setName(name);
        shop.setNumber(String.valueOf(System.nanoTime()).substring(7));
        shop.setCountry(Country.GERMANY);
        shop.setActive(true);
        return shopRepository.save(shop);
    }

    private User saveUser(String username, Role role, Customer customer) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setEmail(username + "@test.com");
        user.setRole(role);
        user.setCustomer(customer);
        user.setActive(true);
        return userRepository.save(user);
    }

    private TestPurchase savePurchase(Customer customer, Shop shop, User mysteryShopper) {
        TestPurchase tp = new TestPurchase();
        // varchar(20). Decimal nanoTime outgrows this prefix; base-36 of any long is at most 13 chars.
        tp.setNumber("TP-T-" + Long.toUnsignedString(System.nanoTime(), 36));
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
        return testPurchaseRepository.save(tp);
    }
}
