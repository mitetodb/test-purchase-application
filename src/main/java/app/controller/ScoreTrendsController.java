package app.controller;

import app.service.QuestionnaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ScoreTrendsController {

    private final QuestionnaireService questionnaireService;

    @GetMapping("/score-trends")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','SALES_MANAGER','CUSTOMER')")
    public String scoreTrends(Model model) {
        model.addAttribute("trendsByMonth", questionnaireService.getScoreTrendsByMonthForCurrentUser());
        model.addAttribute("trendsByShop", questionnaireService.getScoreTrendsByShopForCurrentUser());
        return "score-trends";
    }
}
