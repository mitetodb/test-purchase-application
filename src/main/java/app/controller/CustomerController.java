package app.controller;

import app.model.dto.CustomerDTO;
import app.model.entity.Customer;
import app.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public String listCustomers(Model model) {
        List<Customer> customers = customerService.findAll()
                .stream()
                .sorted(Comparator.comparing(Customer::getNumber).reversed())
                .toList();
        model.addAttribute("customers", customers);
        return "customers/customers-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("customerDTO", new CustomerDTO());
        return "customers/customers-add";
    }

    @PostMapping("/add")
    public String addCustomer(@Valid @ModelAttribute CustomerDTO customerDTO,
                              BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "customers/customers-add";
        }

        customerService.create(customerDTO);
        return "redirect:/customers";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model) {
        Customer customer = customerService.findById(id);

        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setCategory(customer.getCategory());

        model.addAttribute("customerDTO", dto);

        return "customers/customers-edit";
    }

    @PostMapping("/edit/{id}")
    public String editCustomer(@PathVariable UUID id,
                               @Valid @ModelAttribute CustomerDTO dto,
                               BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "customers/customers-edit";
        }

        customerService.update(id, dto);
        return "redirect:/customers";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable UUID id) {
        customerService.delete(id);
        return "redirect:/customers";
    }
}