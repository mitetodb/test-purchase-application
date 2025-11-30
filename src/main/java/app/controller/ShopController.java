package app.controller;

import app.model.dto.ShopDTO;
import app.model.entity.Shop;
import app.service.ShopService;
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
@RequestMapping("/shops")
public class ShopController {

    private final ShopService shopService;

    @GetMapping
    public String list(Model model) {
        List<Shop> shops = shopService.findAll()
                .stream()
                .sorted(Comparator.comparing(Shop::getNumber).reversed())
                .toList();
        model.addAttribute("shops", shops);
        return "shops/shops-list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("shopDTO", new ShopDTO());
        return "shops/shops-add";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("shopDTO") ShopDTO dto,
                      BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "shops/shops-add";
        }

        shopService.create(dto);
        return "redirect:/shops";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable UUID id, Model model) {

        Shop shop = shopService.findById(id);

        ShopDTO dto = new ShopDTO();
        dto.setId(shop.getId());
        dto.setName(shop.getName());
        dto.setCountry(shop.getCountry());
        dto.setNotes(shop.getNotes());
        dto.setDescription(shop.getDescription());

        model.addAttribute("shopDTO", dto);
        return "shops/shops-edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable UUID id,
                       @Valid @ModelAttribute("shopDTO") ShopDTO dto,
                       BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "shops/shops-edit";
        }

        shopService.update(id, dto);
        return "redirect:/shops";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable UUID id) {
        shopService.delete(id);
        return "redirect:/shops";
    }
}
