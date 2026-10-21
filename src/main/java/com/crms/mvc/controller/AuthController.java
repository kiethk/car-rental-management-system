package com.crms.mvc.controller;
import com.crms.mvc.dto.LoginDTO;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Optional;

@Controller
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginForm", new LoginDTO());
        return "auth/login";
    }
    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginDTO form, BindingResult result,
                        HttpSession session, Model model) {
        if (result.hasErrors()) return "auth/login";
        Optional<Customer> customer = authService.login(form.getEmail(), form.getPassword());
        if (customer.isEmpty()) {
            model.addAttribute("error", "Invalid email or password");
            return "auth/login";
        }
        session.setAttribute("customerId", customer.get().getId());
        session.setAttribute("customerName", customer.get().getName());
        return "redirect:/vehicles";
    }
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
