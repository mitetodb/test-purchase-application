package app.service;

import app.model.entity.User;
import app.model.enums.Role;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccessControlService accessControlService;
    private final CustomerRepository customerRepository;
    private final TestPurchaseRepository testPurchaseRepository;
    private final ShopRepository shopRepository;
    private final UserService userService;

    public long getTestPurchaseCount() {
        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN -> testPurchaseRepository.count();
            case ACCOUNT_MANAGER -> getAccountManagerTestPurchaseCount(currentUser.getId());
            case SALES_MANAGER -> getSalesManagerTestPurchaseCount(currentUser.getId());
            case CUSTOMER -> getCustomerTestPurchaseCount(currentUser);
            case MYSTERY_SHOPPER -> getMysteryShopperTestPurchaseCount(currentUser.getId());
        };
    }

    public long getCustomerCount() {
        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN -> customerRepository.count();
            case ACCOUNT_MANAGER -> getAccountManagerCustomerCount(currentUser.getId());
            case SALES_MANAGER -> getSalesManagerCustomerCount(currentUser.getId());
            default -> 0L;
        };
    }

    public long getUserCount() {
        User currentUser = accessControlService.getCurrentUser();
        return currentUser.getRole() == Role.ADMIN ? userService.countUsers() : 0L;
    }

    public long getShopCount() {
        return shopRepository.count();
    }

    private long getAccountManagerTestPurchaseCount(UUID accountManagerId) {
        return testPurchaseRepository.countForAccountManager(accountManagerId);
    }

    private long getSalesManagerTestPurchaseCount(UUID salesManagerId) {
        return testPurchaseRepository.countForSalesManager(salesManagerId);
    }

    private long getCustomerTestPurchaseCount(User customerUser) {
        if (customerUser.getCustomer() == null) {
            return 0L;
        }
        return testPurchaseRepository.countByCustomer_Id(customerUser.getCustomer().getId());
    }

    private long getMysteryShopperTestPurchaseCount(UUID mysteryShopperId) {
        return testPurchaseRepository.countByMysteryShopper_Id(mysteryShopperId);
    }

    private long getAccountManagerCustomerCount(UUID accountManagerId) {
        return customerRepository.countForAccountManager(accountManagerId);
    }

    private long getSalesManagerCustomerCount(UUID salesManagerId) {
        return customerRepository.countBySalesManager_Id(salesManagerId);
    }
}
