package app.service;

import app.TestPurchaseApplication;
import app.model.dto.QuestionnaireSubmitDTO;
import app.model.entity.*;
import app.model.enums.*;
import app.repository.*;
import app.service.impl.QuestionnaireServiceImpl;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class QuestionnaireValidationTest {

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

    private TestPurchase purchase;
    private List<QuestionnaireQuestion> questions;

    @BeforeEach
    void setUp() {
        testPurchaseTestDataCleaner.resetPurchaseGraphAndBusinessFixtures(
                customerRepository, userRepository, shopRepository);

        Customer customer = new Customer();
        customer.setName("Validate Co");
        customer.setNumber("9001");
        customer.setCountry(Country.GERMANY);
        customer.setCategory("GOLD");
        customer.setActive(true);
        customer = customerRepository.save(customer);

        Shop shop = new Shop();
        shop.setName("Validate Shop");
        shop.setNumber("9001");
        shop.setCountry(Country.GERMANY);
        shop.setActive(true);
        shop = shopRepository.save(shop);

        User admin = new User();
        admin.setUsername("admin_validate");
        admin.setPassword("password");
        admin.setEmail("admin_validate@test.com");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);

        purchase = new TestPurchase();
        purchase.setNumber("TP-VAL-1");
        purchase.setCustomer(customer);
        purchase.setShop(shop);
        purchase.setCountry(Country.GERMANY);
        purchase.setCategory(TestPurchaseCategory.S);
        purchase.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
        purchase.setStatus(TestPurchaseStatus.IN_PROGRESS);
        purchase.setActive(true);
        purchase.setProductPrice(50.0);
        purchase.setServiceFee(5.0);
        purchase.setPostageFee(2.0);
        purchase = testPurchaseRepository.save(purchase);

        UUID templateId = templateRepository.findByCodeAndActiveTrue("DEFAULT").orElseThrow().getId();
        questions = questionRepository.findActiveByTemplateId(templateId);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                admin.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void submit_rejectsScoreAboveMax() {
        QuestionnaireSubmitDTO dto = validDto();
        UUID first = questions.get(0).getId();
        dto.getScores().put(first, questions.get(0).getMaxScore() + 1);
        assertThrows(IllegalArgumentException.class,
                () -> questionnaireService.submitQuestionnaire(purchase.getId(), dto));
    }

    @Test
    void submit_requiresAllQuestions() {
        QuestionnaireSubmitDTO dto = new QuestionnaireSubmitDTO();
        dto.getScores().put(questions.get(0).getId(), 3);
        assertThrows(IllegalArgumentException.class,
                () -> questionnaireService.submitQuestionnaire(purchase.getId(), dto));
    }

    @Test
    void computeOverallScore_usesWeightedAveragePercent() {
        Map<UUID, Integer> scores = new LinkedHashMap<>();
        for (QuestionnaireQuestion q : questions) {
            scores.put(q.getId(), q.getMaxScore());
        }
        BigDecimal overall = QuestionnaireServiceImpl.computeOverallScore(questions, scores);
        assertEquals(new BigDecimal("100.00"), overall);
    }

    private QuestionnaireSubmitDTO validDto() {
        QuestionnaireSubmitDTO dto = new QuestionnaireSubmitDTO();
        Map<UUID, Integer> scores = new LinkedHashMap<>();
        for (QuestionnaireQuestion q : questions) {
            scores.put(q.getId(), 3);
        }
        dto.setScores(scores);
        return dto;
    }
}
