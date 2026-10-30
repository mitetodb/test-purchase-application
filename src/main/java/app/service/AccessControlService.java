package app.service;

import app.model.entity.Customer;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.Role;
import app.repository.UserRepository;
import app.config.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + username));
    }

    public User getCurrentUserWithManagedCustomers() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsernameWithManagedCustomers(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + username));
    }

    public boolean canViewTestPurchase(TestPurchase testPurchase) {
        User currentUser = getCurrentUserWithManagedCustomers();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN -> true;
            case SALES_MANAGER -> canSalesManagerViewTestPurchase(testPurchase, currentUser);
            case ACCOUNT_MANAGER -> canAccountManagerViewTestPurchase(testPurchase, currentUser);
            case MYSTERY_SHOPPER -> canMysteryShopperViewTestPurchase(testPurchase, currentUser);
            case CUSTOMER -> canCustomerViewTestPurchase(testPurchase, currentUser);
        };
    }

    public void checkCanViewTestPurchase(TestPurchase testPurchase) {
        if (!canViewTestPurchase(testPurchase)) {
            throw new AccessDeniedException("You are not allowed to view this test purchase");
        }
    }

    public boolean canEditTestPurchase(TestPurchase testPurchase) {
        User currentUser = getCurrentUserWithManagedCustomers();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN -> true;
            case ACCOUNT_MANAGER -> canAccountManagerEditTestPurchase(testPurchase, currentUser);
            default -> false;
        };
    }

    private boolean canAccountManagerEditTestPurchase(TestPurchase testPurchase, User accountManager) {
        return accountManager.getManagedCustomers().stream()
                .anyMatch(c -> c.getId().equals(testPurchase.getCustomer().getId()));
    }

    public void checkCanEditTestPurchase(TestPurchase testPurchase) {
        if (!canEditTestPurchase(testPurchase)) {
            throw new AccessDeniedException("You are not allowed to edit this test purchase");
        }
    }

    public boolean canCreateTestPurchase() {
        User currentUser = getCurrentUser();
        return currentUser.getRole() == Role.ADMIN 
            || currentUser.getRole() == Role.ACCOUNT_MANAGER 
            || currentUser.getRole() == Role.CUSTOMER;
    }

    public void checkCanCreateTestPurchase() {
        if (!canCreateTestPurchase()) {
            throw new AccessDeniedException("You are not allowed to create test purchases");
        }
    }

    public boolean canAssignMysteryShopperToTestPurchase() {
        User currentUser = getCurrentUser();
        return currentUser.getRole() == Role.ADMIN 
            || currentUser.getRole() == Role.ACCOUNT_MANAGER;
    }

    public void checkCanAssignMysteryShopperToTestPurchase() {
        if (!canAssignMysteryShopperToTestPurchase()) {
            throw new AccessDeniedException("You are not allowed to assign mystery shoppers");
        }
    }

    public boolean canChangeTestPurchaseStatus() {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();
        return role == Role.ADMIN || role == Role.ACCOUNT_MANAGER || role == Role.MYSTERY_SHOPPER;
    }

    public void checkCanChangeTestPurchaseStatus() {
        if (!canChangeTestPurchaseStatus()) {
            throw new AccessDeniedException("You are not allowed to change test purchase status");
        }
    }

    public boolean canHardDelete() {
        User currentUser = getCurrentUser();
        return currentUser.getRole() == Role.ADMIN;
    }

    public void checkCanHardDelete() {
        if (!canHardDelete()) {
            throw new AccessDeniedException("Only admins can hard-delete");
        }
    }

    public boolean canViewCustomer(Customer customer) {
        User currentUser = getCurrentUserWithManagedCustomers();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN -> true;
            case SALES_MANAGER -> canSalesManagerViewCustomer(customer, currentUser);
            case ACCOUNT_MANAGER -> canAccountManagerViewCustomer(customer, currentUser);
            default -> false;
        };
    }

    public void checkCanViewCustomer(Customer customer) {
        if (!canViewCustomer(customer)) {
            throw new AccessDeniedException("You are not allowed to view this customer");
        }
    }

    public boolean canEditCustomer(Customer customer) {
        User currentUser = getCurrentUserWithManagedCustomers();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN -> true;
            case ACCOUNT_MANAGER -> canAccountManagerEditCustomer(customer, currentUser);
            case SALES_MANAGER -> canSalesManagerViewCustomer(customer, currentUser);
            default -> false;
        };
    }

    private boolean canAccountManagerEditCustomer(Customer customer, User accountManager) {
        return accountManager.getManagedCustomers().stream()
                .anyMatch(c -> c.getId().equals(customer.getId()));
    }

    public void checkCanEditCustomer(Customer customer) {
        if (!canEditCustomer(customer)) {
            throw new AccessDeniedException("You are not allowed to edit this customer");
        }
    }

    public boolean canSetCustomerPricing() {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();
        return role == Role.ADMIN || role == Role.ACCOUNT_MANAGER || role == Role.SALES_MANAGER;
    }

    public boolean shouldHideMysteryShopperName() {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();
        return role == Role.SALES_MANAGER || role == Role.CUSTOMER;
    }

    private boolean canSalesManagerViewTestPurchase(TestPurchase testPurchase, User salesManager) {
        if (testPurchase.getCustomer() == null) {
            return false;
        }
        Customer customer = testPurchase.getCustomer();
        return customer.getSalesManager() != null 
            && customer.getSalesManager().getId().equals(salesManager.getId());
    }

    private boolean canAccountManagerViewTestPurchase(TestPurchase testPurchase, User accountManager) {
        if (testPurchase.getCustomer() == null) {
            return false;
        }
        Customer customer = testPurchase.getCustomer();
        return accountManager.getManagedCustomers() != null 
            && accountManager.getManagedCustomers().contains(customer);
    }

    private boolean canMysteryShopperViewTestPurchase(TestPurchase testPurchase, User mysteryShopper) {
        return testPurchase.getMysteryShopper() != null 
            && testPurchase.getMysteryShopper().getId().equals(mysteryShopper.getId());
    }

    private boolean canCustomerViewTestPurchase(TestPurchase testPurchase, User customerUser) {
        if (customerUser.getCustomer() == null || testPurchase.getCustomer() == null) {
            return false;
        }
        return testPurchase.getCustomer().getId().equals(customerUser.getCustomer().getId());
    }

    private boolean canSalesManagerViewCustomer(Customer customer, User salesManager) {
        return customer.getSalesManager() != null 
            && customer.getSalesManager().getId().equals(salesManager.getId());
    }

    private boolean canAccountManagerViewCustomer(Customer customer, User accountManager) {
        return accountManager.getManagedCustomers() != null 
            && accountManager.getManagedCustomers().contains(customer);
    }
}
