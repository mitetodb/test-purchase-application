package app.repository;

import app.model.entity.QuestionnaireQuestion;
import app.model.entity.QuestionnaireTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireTemplateRepository extends JpaRepository<QuestionnaireTemplate, UUID> {

    Optional<QuestionnaireTemplate> findByCodeAndActiveTrue(String code);

    @Query("""
        SELECT t FROM QuestionnaireTemplate t
        WHERE t.active = true
        ORDER BY t.code
        """)
    List<QuestionnaireTemplate> findAllActive();
}
