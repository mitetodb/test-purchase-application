package app.controller;

import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final CustomerRepository customerRepository;
    private final TestPurchaseRepository testPurchaseRepository;
    private final ShopRepository shopRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.countUsers());
        model.addAttribute("totalCustomers", customerRepository.count());
        model.addAttribute("totalTestPurchases", testPurchaseRepository.count());
        model.addAttribute("totalShops", shopRepository.count());
        return "dashboard";
    }
}
