package com.crms.mvc.controller;

import com.crms.mvc.dto.LoginDTO;
import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.dto.CustomerRegisterDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.service.AuthService;
import com.crms.mvc.service.CustomerService;
import com.crms.mvc.service.SessionAccountService;
import com.crms.mvc.util.CurrentAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final AuthService authService;
    private final CustomerService customerService;
    private final SessionAccountService sessionAccountService;

    public AuthController(AuthService authService,
                          CustomerService customerService,
                          SessionAccountService sessionAccountService) {
        this.authService = authService;
        this.customerService = customerService;
        this.sessionAccountService = sessionAccountService;
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginForm", new LoginDTO());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginDTO form,
                        BindingResult result,
                        HttpServletRequest request,
                        Model model) {
        if (result.hasErrors()) {
            return "auth/login";
        }

        LoginIdentityDTO account = authService.login(form.getEmail(), form.getPassword());
        if (account == null) {
            model.addAttribute("error", "Invalid email or password");
            return "auth/login";
        }

        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        SessionAccount sessionAccount = sessionAccountService.create(account);
        HttpSession session = request.getSession(true);
        session.setAttribute(CurrentAccount.SESSION_KEY, sessionAccount);
        return sessionAccount.isCustomer() ? "redirect:/vehicles" : "redirect:/admin/dashboard";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new CustomerRegisterDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") CustomerRegisterDTO form,
                           BindingResult result,
                           Model model) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        if (!customerService.register(form)) {
            model.addAttribute("error", "Email is already in use");
            return "auth/register";
        }

        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
