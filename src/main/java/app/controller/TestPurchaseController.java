package app.controller;

import app.model.dto.TestPurchaseCreateDTO;
import app.model.dto.TestPurchaseEditDTO;
import app.model.entity.TestPurchase;
import app.service.AttachmentService;
import app.service.CustomerService;
import app.service.ShopService;
import app.service.TestPurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public String list(Model model) {
        List<TestPurchase> purchases = testPurchaseService.findAll();
        model.addAttribute("purchases", purchases);
        return "testpurchases/testpurchases-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("dto")) {
            TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
            dto.getItems().add(new app.model.dto.ItemDTO());
            model.addAttribute("dto", dto);
        }

        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("shops", shopService.findAll());
        return "testpurchases/testpurchase-add";
    }

    @PostMapping("/add")
    public String add(
            @Valid @ModelAttribute("dto") TestPurchaseCreateDTO dto,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customers", customerService.findAll());
            model.addAttribute("shops", shopService.findAll());
            return "testpurchases/testpurchase-add";
        }

        testPurchaseService.create(dto);
        return "redirect:/testpurchases";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model) {
        var purchase = testPurchaseService.findById(id);

        TestPurchaseEditDTO dto = new TestPurchaseEditDTO();
        dto.setId(purchase.getId());
        dto.setCustomerId(purchase.getCustomer().getId());
        dto.setShopId(purchase.getShop().getId());

        purchase.getItems().forEach(i -> {
            app.model.dto.ItemDTO itemDTO = new app.model.dto.ItemDTO();
            itemDTO.setId(i.getId());
            itemDTO.setProductName(i.getProductName());
            itemDTO.setQuantity(i.getQuantity());
            itemDTO.setUnitPrice(i.getUnitPrice());
            dto.getItems().add(itemDTO);
        });

        model.addAttribute("dto", dto);
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("shops", shopService.findAll());
        model.addAttribute("purchase", purchase);
        return "testpurchases/testpurchase-edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(
            @PathVariable UUID id,
            @Valid @ModelAttribute("dto") TestPurchaseEditDTO dto,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customers", customerService.findAll());
            model.addAttribute("shops", shopService.findAll());
            return "testpurchases/testpurchase-edit";
        }

        testPurchaseService.edit(id, dto);
        return "redirect:/testpurchases";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        testPurchaseService.delete(id);
        return "redirect:/testpurchases";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable UUID id, Model model) {
        TestPurchase purchase = testPurchaseService.findById(id);
        model.addAttribute("purchase", purchase);
        model.addAttribute("attachments", attachmentService.getByTestPurchase(id));
        return "testpurchases/testpurchase-view";
    }
}
