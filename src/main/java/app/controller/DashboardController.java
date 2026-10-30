package app.controller;

import app.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", dashboardService.getUserCount());
        model.addAttribute("totalCustomers", dashboardService.getCustomerCount());
        model.addAttribute("totalTestPurchases", dashboardService.getTestPurchaseCount());
        model.addAttribute("totalShops", dashboardService.getShopCount());
        return "dashboard";
    }
}
