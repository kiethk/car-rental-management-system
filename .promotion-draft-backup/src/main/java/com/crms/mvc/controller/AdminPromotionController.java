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
        return "admin/promotions/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("promotionForm")) {
            model.addAttribute("promotionForm", new PromotionFormDTO());
        }
        model.addAttribute("activeNav", "promotions");
        return "admin/promotions/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("promotionForm") PromotionFormDTO form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            form.setUrlImagePromotion(null);
            model.addAttribute("activeNav", "promotions");
            return "admin/promotions/form";
        }
        try {
            Promotion promotion = promotionService.create(form);
            ra.addFlashAttribute("success", "Promotion " + promotion.getCode() + " created successfully.");
            return "redirect:/admin/promotions";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            form.setUrlImagePromotion(null);
            model.addAttribute("activeNav", "promotions");
            return "admin/promotions/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Promotion promotion = promotionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with ID: " + id));
        if (!model.containsAttribute("promotionForm")) {
            model.addAttribute("promotionForm", PromotionFormDTO.fromEntity(promotion));
        }
        model.addAttribute("promotion", promotion);
        model.addAttribute("activeNav", "promotions");
        return "admin/promotions/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("promotionForm") PromotionFormDTO form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        form.setId(id);
        if (result.hasErrors()) {
            promotionService.findById(id).ifPresent(promotion ->
                    form.setUrlImagePromotion(promotion.getUrlImagePromotion()));
            model.addAttribute("activeNav", "promotions");
            return "admin/promotions/form";
        }
        try {
            Promotion promotion = promotionService.update(id, form);
            ra.addFlashAttribute("success", "Promotion " + promotion.getCode() + " updated successfully.");
            return "redirect:/admin/promotions";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            promotionService.findById(id).ifPresent(promotion ->
                    form.setUrlImagePromotion(promotion.getUrlImagePromotion()));
            model.addAttribute("activeNav", "promotions");
            return "admin/promotions/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            promotionService.delete(id);
            ra.addFlashAttribute("success", "Promotion deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/promotions";
    }
}
