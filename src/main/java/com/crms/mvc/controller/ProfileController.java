package com.crms.mvc.controller;

import com.crms.mvc.dto.CustomerProfileDTO;
import com.crms.mvc.dto.PasswordChangeDTO;
import com.crms.mvc.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProfileController {
    private final CustomerService customerService;

    public ProfileController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/admin/licenses";
        }

        CustomerProfileDTO profile = customerService.getProfile(customerId);
        if (profile == null) {
            return "redirect:/logout";
        }

        model.addAttribute("profileForm", profile);
        model.addAttribute("passwordForm", new PasswordChangeDTO());
        return "profile/index";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") CustomerProfileDTO form,
                                BindingResult result,
                                HttpSession session,
                                Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/admin/licenses";
        }

        if (result.hasErrors()) {
            model.addAttribute("passwordForm", new PasswordChangeDTO());
            return "profile/index";
        }

        if (!customerService.updateProfile(customerId, form)) {
            model.addAttribute("error", "Could not update profile");
            model.addAttribute("passwordForm", new PasswordChangeDTO());
            return "profile/index";
        }

        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeDTO form,
                                 BindingResult result,
                                 HttpSession session,
                                 Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/admin/licenses";
        }

        CustomerProfileDTO profile = customerService.getProfile(customerId);
        if (profile == null) {
            return "redirect:/logout";
        }

        if (result.hasErrors()) {
            model.addAttribute("profileForm", profile);
            return "profile/index";
        }

        if (!customerService.changePassword(customerId, form)) {
            model.addAttribute("passwordError", "Current password is incorrect");
            model.addAttribute("profileForm", profile);
            return "profile/index";
        }

        return "redirect:/profile";
    }
}
