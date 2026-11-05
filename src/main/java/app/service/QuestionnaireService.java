package app.service;

import app.model.dto.QuestionnaireResultViewDTO;
import app.model.dto.QuestionnaireSubmitDTO;
import app.model.dto.ScoreTrendMonthDTO;
import app.model.dto.ScoreTrendShopDTO;
import app.model.entity.TestPurchase;

import java.util.List;
import java.util.UUID;

public interface QuestionnaireService {

    QuestionnaireResultViewDTO buildFormForTestPurchase(UUID testPurchaseId);

    QuestionnaireResultViewDTO getResultForTestPurchase(UUID testPurchaseId);

    void submitQuestionnaire(UUID testPurchaseId, QuestionnaireSubmitDTO dto);

    List<ScoreTrendMonthDTO> getScoreTrendsByMonthForCurrentUser();

    List<ScoreTrendShopDTO> getScoreTrendsByShopForCurrentUser();

    boolean hasResponse(UUID testPurchaseId);

    void validateSubmitDto(TestPurchase testPurchase, QuestionnaireSubmitDTO dto);
}
