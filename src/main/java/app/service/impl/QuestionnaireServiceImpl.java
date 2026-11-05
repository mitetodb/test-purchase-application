package app.service.impl;

import app.model.dto.QuestionnaireQuestionViewDTO;
import app.model.dto.QuestionnaireResultViewDTO;
import app.model.dto.QuestionnaireSubmitDTO;
import app.model.dto.ScoreTrendMonthDTO;
import app.model.dto.ScoreTrendShopDTO;
import app.model.entity.*;
import app.model.enums.TestPurchaseStatus;
import app.repository.QuestionnaireQuestionRepository;
import app.repository.QuestionnaireTemplateRepository;
import app.repository.TestPurchaseQuestionnaireResponseRepository;
import app.repository.TestPurchaseRepository;
import app.service.AccessControlService;
import app.service.QuestionnaireService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionnaireServiceImpl implements QuestionnaireService {

    static final String DEFAULT_TEMPLATE_CODE = "DEFAULT";

    private final TestPurchaseRepository testPurchaseRepository;
    private final QuestionnaireTemplateRepository templateRepository;
    private final QuestionnaireQuestionRepository questionRepository;
    private final TestPurchaseQuestionnaireResponseRepository responseRepository;
    private final AccessControlService accessControlService;
    private final MessageSource messageSource;

    @Override
    public QuestionnaireResultViewDTO buildFormForTestPurchase(UUID testPurchaseId) {
        TestPurchase purchase = loadPurchase(testPurchaseId);
        accessControlService.checkCanFillQuestionnaire(purchase);
        ensureNotArchived(purchase);

        QuestionnaireTemplate template = requireDefaultTemplate();
        List<QuestionnaireQuestion> questions = questionRepository.findActiveByTemplateId(template.getId());
        Optional<TestPurchaseQuestionnaireResponse> existing = responseRepository.findByTestPurchase_Id(testPurchaseId);
        Map<UUID, Integer> existingScores = existing
                .map(r -> r.getAnswers().stream()
                        .collect(Collectors.toMap(a -> a.getQuestion().getId(), TestPurchaseQuestionnaireAnswer::getScore)))
                .orElse(Map.of());

        List<QuestionnaireQuestionViewDTO> questionViews = questions.stream()
                .map(q -> toQuestionView(q, existingScores.get(q.getId())))
                .toList();

        return QuestionnaireResultViewDTO.builder()
                .testPurchaseId(purchase.getId())
                .testPurchaseNumber(purchase.getNumber())
                .overallScore(existing.map(TestPurchaseQuestionnaireResponse::getOverallScore).orElse(null))
                .submittedAt(existing.map(TestPurchaseQuestionnaireResponse::getSubmittedAt).orElse(null))
                .hideEvaluatorIdentity(false)
                .evaluatorDisplayName(existing.flatMap(r -> optionalEvaluatorName(r)).orElse(null))
                .questions(questionViews)
                .build();
    }

    @Override
    public QuestionnaireResultViewDTO getResultForTestPurchase(UUID testPurchaseId) {
        TestPurchase purchase = loadPurchase(testPurchaseId);
        accessControlService.checkCanViewTestPurchase(purchase);

        TestPurchaseQuestionnaireResponse response = responseRepository.findDetailedByTestPurchase_Id(testPurchaseId)
                .orElseThrow(() -> new EntityNotFoundException("Questionnaire not submitted for this test purchase"));

        boolean hideIdentity = accessControlService.shouldHideMysteryShopperName();
        List<QuestionnaireQuestionViewDTO> questionViews = response.getAnswers().stream()
                .sorted(Comparator.comparing(a -> a.getQuestion().getSortOrder()))
                .map(a -> toQuestionView(a.getQuestion(), a.getScore()))
                .toList();

        return QuestionnaireResultViewDTO.builder()
                .testPurchaseId(purchase.getId())
                .testPurchaseNumber(purchase.getNumber())
                .overallScore(response.getOverallScore())
                .submittedAt(response.getSubmittedAt())
                .hideEvaluatorIdentity(hideIdentity)
                .evaluatorDisplayName(hideIdentity ? null : optionalEvaluatorName(response).orElse(null))
                .questions(questionViews)
                .build();
    }

    @Override
    @Transactional
    public void submitQuestionnaire(UUID testPurchaseId, QuestionnaireSubmitDTO dto) {
        TestPurchase purchase = loadPurchase(testPurchaseId);
        accessControlService.checkCanFillQuestionnaire(purchase);
        ensureNotArchived(purchase);
        validateSubmitDto(purchase, dto);

        QuestionnaireTemplate template = requireDefaultTemplate();
        List<QuestionnaireQuestion> questions = questionRepository.findActiveByTemplateId(template.getId());
        BigDecimal overall = computeOverallScore(questions, dto.getScores());

        User currentUser = accessControlService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        TestPurchaseQuestionnaireResponse response = responseRepository.findByTestPurchase_Id(testPurchaseId)
                .orElseGet(() -> {
                    TestPurchaseQuestionnaireResponse created = new TestPurchaseQuestionnaireResponse();
                    created.setTestPurchase(purchase);
                    created.setTemplate(template);
                    created.setAnswers(new ArrayList<>());
                    return created;
                });

        response.setOverallScore(overall);
        response.setSubmittedAt(now);
        response.setUpdatedOn(now);
        response.setSubmittedBy(currentUser);
        response.setTemplate(template);

        response.getAnswers().clear();
        for (QuestionnaireQuestion question : questions) {
            Integer score = dto.getScores().get(question.getId());
            TestPurchaseQuestionnaireAnswer answer = TestPurchaseQuestionnaireAnswer.builder()
                    .response(response)
                    .question(question)
                    .score(score)
                    .build();
            response.getAnswers().add(answer);
        }

        responseRepository.save(response);
    }

    @Override
    public void validateSubmitDto(TestPurchase testPurchase, QuestionnaireSubmitDTO dto) {
        if (dto == null || dto.getScores() == null || dto.getScores().isEmpty()) {
            throw new IllegalArgumentException("Questionnaire scores are required");
        }
        QuestionnaireTemplate template = requireDefaultTemplate();
        List<QuestionnaireQuestion> questions = questionRepository.findActiveByTemplateId(template.getId());
        if (dto.getScores().size() != questions.size()) {
            throw new IllegalArgumentException("All questionnaire questions must be scored");
        }
        Set<UUID> questionIds = questions.stream().map(QuestionnaireQuestion::getId).collect(Collectors.toSet());
        for (Map.Entry<UUID, Integer> entry : dto.getScores().entrySet()) {
            if (!questionIds.contains(entry.getKey())) {
                throw new IllegalArgumentException("Unknown questionnaire question");
            }
            QuestionnaireQuestion q = questions.stream()
                    .filter(question -> question.getId().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow();
            int score = entry.getValue();
            if (score < 0 || score > q.getMaxScore()) {
                throw new IllegalArgumentException("Score must be between 0 and " + q.getMaxScore());
            }
        }
    }

    @Override
    public boolean hasResponse(UUID testPurchaseId) {
        return responseRepository.findByTestPurchase_Id(testPurchaseId).isPresent();
    }

    @Override
    public List<ScoreTrendMonthDTO> getScoreTrendsByMonthForCurrentUser() {
        accessControlService.checkCanViewScoreTrends();
        List<TestPurchaseQuestionnaireResponse> scoped = scopedResponsesForCurrentUser();
        Map<YearMonth, List<BigDecimal>> grouped = scoped.stream()
                .collect(Collectors.groupingBy(
                        r -> YearMonth.from(r.getSubmittedAt()),
                        Collectors.mapping(TestPurchaseQuestionnaireResponse::getOverallScore, Collectors.toList())));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> ScoreTrendMonthDTO.builder()
                        .year(e.getKey().getYear())
                        .month(e.getKey().getMonthValue())
                        .averageScore(average(e.getValue()))
                        .responseCount(e.getValue().size())
                        .build())
                .toList();
    }

    @Override
    public List<ScoreTrendShopDTO> getScoreTrendsByShopForCurrentUser() {
        accessControlService.checkCanViewScoreTrends();
        List<TestPurchaseQuestionnaireResponse> scoped = scopedResponsesForCurrentUser();
        Map<UUID, List<TestPurchaseQuestionnaireResponse>> byShop = scoped.stream()
                .collect(Collectors.groupingBy(r -> r.getTestPurchase().getShop().getId()));

        return byShop.values().stream()
                .map(responses -> {
                    TestPurchase sample = responses.get(0).getTestPurchase();
                    List<BigDecimal> scores = responses.stream()
                            .map(TestPurchaseQuestionnaireResponse::getOverallScore)
                            .toList();
                    return ScoreTrendShopDTO.builder()
                            .shopId(sample.getShop().getId())
                            .shopName(sample.getShop().getName())
                            .averageScore(average(scores))
                            .responseCount(responses.size())
                            .build();
                })
                .sorted(Comparator.comparing(ScoreTrendShopDTO::getShopName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<TestPurchaseQuestionnaireResponse> scopedResponsesForCurrentUser() {
        List<TestPurchaseQuestionnaireResponse> all = responseRepository.findAllForTrendsWithPurchase();
        return all.stream()
                .filter(r -> accessControlService.canViewTestPurchase(r.getTestPurchase()))
                .toList();
    }

    public static BigDecimal computeOverallScore(List<QuestionnaireQuestion> questions, Map<UUID, Integer> scores) {
        int weightSum = 0;
        BigDecimal weighted = BigDecimal.ZERO;
        for (QuestionnaireQuestion q : questions) {
            int score = scores.get(q.getId());
            int weight = q.getWeight();
            weightSum += weight;
            if (q.getMaxScore() <= 0) {
                continue;
            }
            BigDecimal normalized = BigDecimal.valueOf(score)
                    .divide(BigDecimal.valueOf(q.getMaxScore()), 6, RoundingMode.HALF_UP);
            weighted = weighted.add(normalized.multiply(BigDecimal.valueOf(weight)));
        }
        if (weightSum == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return weighted
                .divide(BigDecimal.valueOf(weightSum), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private QuestionnaireQuestionViewDTO toQuestionView(QuestionnaireQuestion q, Integer givenScore) {
        String prompt = messageSource.getMessage(q.getMessageKey(), null, q.getMessageKey(), LocaleContextHolder.getLocale());
        return QuestionnaireQuestionViewDTO.builder()
                .id(q.getId())
                .sortOrder(q.getSortOrder())
                .prompt(prompt)
                .maxScore(q.getMaxScore())
                .weight(q.getWeight())
                .givenScore(givenScore)
                .build();
    }

    private Optional<String> optionalEvaluatorName(TestPurchaseQuestionnaireResponse response) {
        if (response.getSubmittedBy() == null) {
            return Optional.empty();
        }
        return Optional.of(response.getSubmittedBy().getUsername());
    }

    private TestPurchase loadPurchase(UUID id) {
        return testPurchaseRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new EntityNotFoundException("Test purchase not found"));
    }

    private QuestionnaireTemplate requireDefaultTemplate() {
        return templateRepository.findByCodeAndActiveTrue(DEFAULT_TEMPLATE_CODE)
                .orElseThrow(() -> new IllegalStateException("Default questionnaire template is missing"));
    }

    private static void ensureNotArchived(TestPurchase purchase) {
        if (purchase.getStatus() == TestPurchaseStatus.ARCHIVED) {
            throw new IllegalStateException("Archived test purchases are read-only");
        }
    }
}
