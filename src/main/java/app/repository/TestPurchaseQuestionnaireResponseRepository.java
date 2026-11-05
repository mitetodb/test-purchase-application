package app.repository;

import app.model.entity.TestPurchaseQuestionnaireResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestPurchaseQuestionnaireResponseRepository extends JpaRepository<TestPurchaseQuestionnaireResponse, UUID> {

    Optional<TestPurchaseQuestionnaireResponse> findByTestPurchase_Id(UUID testPurchaseId);

    @Query("""
        SELECT DISTINCT r FROM TestPurchaseQuestionnaireResponse r
        LEFT JOIN FETCH r.submittedBy
        LEFT JOIN FETCH r.answers a
        LEFT JOIN FETCH a.question
        WHERE r.testPurchase.id = :testPurchaseId
        """)
    Optional<TestPurchaseQuestionnaireResponse> findDetailedByTestPurchase_Id(UUID testPurchaseId);

    @Query("""
        SELECT DISTINCT r FROM TestPurchaseQuestionnaireResponse r
        JOIN FETCH r.testPurchase tp
        JOIN FETCH tp.shop s
        JOIN FETCH tp.customer c
        LEFT JOIN FETCH r.answers a
        LEFT JOIN FETCH a.question
        WHERE tp.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
          AND tp.active = true
        """)
    List<TestPurchaseQuestionnaireResponse> findAllForTrendsWithPurchase();
}
