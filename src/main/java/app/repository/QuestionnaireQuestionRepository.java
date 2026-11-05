package app.repository;

import app.model.entity.QuestionnaireQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface QuestionnaireQuestionRepository extends JpaRepository<QuestionnaireQuestion, UUID> {

    @Query("""
        SELECT q FROM QuestionnaireQuestion q
        WHERE q.template.id = :templateId
          AND q.active = true
        ORDER BY q.sortOrder ASC
        """)
    List<QuestionnaireQuestion> findActiveByTemplateId(UUID templateId);
}
