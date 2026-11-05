package app.controller;

import app.model.dto.QuestionnaireSubmitDTO;
import app.service.QuestionnaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/testpurchases/{testPurchaseId}/questionnaire")
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','MYSTERY_SHOPPER')")
    public String showForm(@PathVariable UUID testPurchaseId, Model model) {
        model.addAttribute("questionnaire", questionnaireService.buildFormForTestPurchase(testPurchaseId));
        return "testpurchases/questionnaire-form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','MYSTERY_SHOPPER')")
    public String submit(@PathVariable UUID testPurchaseId,
                         @RequestParam Map<String, String> params,
                         RedirectAttributes redirectAttributes) {
        QuestionnaireSubmitDTO dto = new QuestionnaireSubmitDTO();
        dto.setScores(parseScores(params));
        try {
            questionnaireService.submitQuestionnaire(testPurchaseId, dto);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("questionnaireError", ex.getMessage());
            return "redirect:/testpurchases/" + testPurchaseId + "/questionnaire";
        }
        return "redirect:/testpurchases/view/" + testPurchaseId;
    }

    private static Map<UUID, Integer> parseScores(Map<String, String> params) {
        Map<UUID, Integer> scores = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getKey().startsWith("score_")) {
                continue;
            }
            UUID questionId = UUID.fromString(entry.getKey().substring("score_".length()));
            scores.put(questionId, Integer.parseInt(entry.getValue()));
        }
        return scores;
    }
}
