package com.crms.mvc.controller;

import com.crms.mvc.dto.PasswordChangeDTO;
import com.crms.mvc.service.EmployeeService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class EmployeePasswordController {
    private final EmployeeService employeeService;

    public EmployeePasswordController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/admin/password")
    public String passwordForm(Model model) {
        model.addAttribute("passwordForm", new PasswordChangeDTO());
        return "admin/password";
    }

    @PostMapping("/admin/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeDTO form,
                                 BindingResult result,
                                 HttpSession session,
                                 Model model) {
        if (result.hasErrors()) {
            return "admin/password";
        }

        Long employeeId = (Long) session.getAttribute("employeeId");
        if (!employeeService.changePassword(employeeId, form)) {
            model.addAttribute("error", "Current password is incorrect");
            return "admin/password";
        }

        return "redirect:/admin/password?changed";
    }
}
