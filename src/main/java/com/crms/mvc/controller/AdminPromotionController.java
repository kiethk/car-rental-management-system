package com.crms.mvc.controller;

import com.crms.mvc.dto.PromotionFormDTO;
import com.crms.mvc.entity.Promotion;
import com.crms.mvc.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/promotions")
public class AdminPromotionController {
    private final PromotionService promotionService;

    public AdminPromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("promotions", promotionService.findAll());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("activeNav", "promotions");
        return "screens/admin/promotions/list/index";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("promotionForm")) {
            model.addAttribute("promotionForm", new PromotionFormDTO());
        }
        model.addAttribute("activeNav", "promotions");
        return "screens/admin/promotions/form/index";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("promotionForm") PromotionFormDTO form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            form.setUrlImagePromotion(null);
            model.addAttribute("activeNav", "promotions");
            return "screens/admin/promotions/form/index";
        }
        try {
            Promotion promotion = promotionService.create(form);
            redirectAttributes.addFlashAttribute("success",
                    "Promotion " + promotion.getCode() + " created successfully.");
            return "redirect:/admin/promotions";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            form.setUrlImagePromotion(null);
            model.addAttribute("activeNav", "promotions");
            return "screens/admin/promotions/form/index";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Promotion promotion = promotionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi có mã: " + id));
        if (!model.containsAttribute("promotionForm")) {
            model.addAttribute("promotionForm", PromotionFormDTO.fromEntity(promotion));
        }
        model.addAttribute("promotion", promotion);
        model.addAttribute("activeNav", "promotions");
        return "screens/admin/promotions/form/index";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("promotionForm") PromotionFormDTO form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        form.setId(id);
        if (result.hasErrors()) {
            promotionService.findById(id).ifPresent(promotion ->
                    form.setUrlImagePromotion(promotion.getUrlImagePromotion()));
            model.addAttribute("activeNav", "promotions");
            return "screens/admin/promotions/form/index";
        }
        try {
            Promotion promotion = promotionService.update(id, form);
            redirectAttributes.addFlashAttribute("success",
                    "Promotion " + promotion.getCode() + " updated successfully.");
            return "redirect:/admin/promotions";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            promotionService.findById(id).ifPresent(promotion ->
                    form.setUrlImagePromotion(promotion.getUrlImagePromotion()));
            model.addAttribute("activeNav", "promotions");
            return "screens/admin/promotions/form/index";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa khuyến mãi.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/promotions";
    }
}
