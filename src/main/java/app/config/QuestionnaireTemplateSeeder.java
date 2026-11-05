package app.config;

import app.model.entity.QuestionnaireQuestion;
import app.model.entity.QuestionnaireTemplate;
import app.repository.QuestionnaireQuestionRepository;
import app.repository.QuestionnaireTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Ensures the default evaluation questionnaire exists when Flyway is disabled (e.g. H2 tests).
 * Idempotent: skipped when {@code DEFAULT} template is already present (including Flyway V14 seed).
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class QuestionnaireTemplateSeeder implements CommandLineRunner {

    private final QuestionnaireTemplateRepository templateRepository;
    private final QuestionnaireQuestionRepository questionRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(String... args) {
        if (templateRepository.findByCodeAndActiveTrue("DEFAULT").isPresent()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> seedDefaultTemplate());
        log.info("Seeded default questionnaire template and starter questions.");
    }

    private void seedDefaultTemplate() {
        if (templateRepository.findByCodeAndActiveTrue("DEFAULT").isPresent()) {
            return;
        }
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .code("DEFAULT")
                .active(true)
                .build();
        templateRepository.save(template);

        List<QuestionnaireQuestion> questions = List.of(
                question(template, 1, "questionnaire.q.product_availability", 5, 2),
                question(template, 2, "questionnaire.q.staff_knowledge", 5, 2),
                question(template, 3, "questionnaire.q.checkout_experience", 5, 1),
                question(template, 4, "questionnaire.q.store_presentation", 5, 1),
                question(template, 5, "questionnaire.q.overall_satisfaction", 5, 2));
        questionRepository.saveAll(questions);
    }

    private static QuestionnaireQuestion question(
            QuestionnaireTemplate template, int sortOrder, String messageKey, int maxScore, int weight) {
        return QuestionnaireQuestion.builder()
                .template(template)
                .sortOrder(sortOrder)
                .messageKey(messageKey)
                .maxScore(maxScore)
                .weight(weight)
                .active(true)
                .build();
    }
}
