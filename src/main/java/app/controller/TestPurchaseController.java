package app.controller;

import app.model.dto.TestPurchaseCreateDTO;
import app.model.dto.TestPurchaseEditDTO;
import app.model.entity.TestPurchase;
import app.model.enums.TestPurchaseStatus;
import app.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/testpurchases")
public class TestPurchaseController {

    private final TestPurchaseService testPurchaseService;
    private final CustomerService customerService;
    private final ShopService shopService;
    private final AttachmentService attachmentService;
    private final StatusHistoryService statusHistoryService;

    @GetMapping
    public String list(Model model) {
        List<TestPurchase> purchases = testPurchaseService.findAllForCurrentUser();
        model.addAttribute("purchases", purchases);
        return "testpurchases/testpurchases-list";
    }

    @GetMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','CUSTOMER')")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("dto")) {
            TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
            dto.getItems().add(new app.model.dto.ItemDTO());
            model.addAttribute("dto", dto);
        }

        model.addAttribute("customers", customerService.findAllForCurrentUser());
        model.addAttribute("shops", shopService.findAll());
        return "testpurchases/testpurchase-add";
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','CUSTOMER')")
    public String add(
            @Valid @ModelAttribute("dto") TestPurchaseCreateDTO dto,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customers", customerService.findAllForCurrentUser());
            model.addAttribute("shops", shopService.findAll());
            return "testpurchases/testpurchase-add";
        }

        testPurchaseService.create(dto);
        return "redirect:/testpurchases";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER')")
    public String showEditForm(@PathVariable UUID id, Model model) {
        var purchase = testPurchaseService.findById(id);

        TestPurchaseEditDTO dto = new TestPurchaseEditDTO();
        dto.setId(purchase.getId());
        dto.setCustomerId(purchase.getCustomer().getId());
        dto.setShopId(purchase.getShop().getId());

        purchase.getItems().forEach(i -> {
            app.model.dto.ItemDTO itemDTO = new app.model.dto.ItemDTO();
            itemDTO.setId(i.getId());
            itemDTO.setProductUrl(i.getProductUrl());
            itemDTO.setProductName(i.getProductName());
            itemDTO.setQuantity(i.getQuantity());
            itemDTO.setUnitPrice(i.getUnitPrice());
            dto.getItems().add(itemDTO);
        });

        model.addAttribute("dto", dto);
        model.addAttribute("customers", customerService.findAllForCurrentUser());
        model.addAttribute("shops", shopService.findAll());
        model.addAttribute("purchase", purchase);
        return "testpurchases/testpurchase-edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER')")
    public String edit(
            @PathVariable UUID id,
            @Valid @ModelAttribute("dto") TestPurchaseEditDTO dto,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customers", customerService.findAllForCurrentUser());
            model.addAttribute("shops", shopService.findAll());
            return "testpurchases/testpurchase-edit";
        }
        model.addAttribute("testPurchaseDTO", dto);
        testPurchaseService.edit(id, dto);
        return "redirect:/testpurchases";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable UUID id) {
        testPurchaseService.delete(id);
        return "redirect:/testpurchases";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable UUID id, Model model) {
        TestPurchase purchase = testPurchaseService.findByIdForCurrentUser(id);
        model.addAttribute("purchase", purchase);
        model.addAttribute("attachments", attachmentService.getByTestPurchase(id));
        model.addAttribute("history", statusHistoryService.getHistory(id));
        model.addAttribute("statuses", TestPurchaseStatus.values());
        return "testpurchases/testpurchase-view";
    }

    @PostMapping("/{id}/change-status")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','MYSTERY_SHOPPER')")
    public String changeStatus(@PathVariable UUID id,
                               @RequestParam TestPurchaseStatus newStatus,
                               @RequestParam(required = false) String comment) {

        testPurchaseService.changeStatus(id, newStatus, comment);

        return "redirect:/testpurchases/view/" + id;
    }

}
