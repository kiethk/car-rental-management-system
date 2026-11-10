package com.crms.mvc.controller;

import com.crms.mvc.dto.LoginDTO;
import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.dto.CustomerRegisterDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.exception.AccountLockedException;
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
    public String loginForm(HttpServletRequest request, Model model) {
        SessionAccount current = CurrentAccount.of(request.getSession(false)).orElse(null);
        if (current != null) {
            return current.isCustomer() ? "redirect:/" : "redirect:/admin/dashboard";
        }
        model.addAttribute("loginForm", new LoginDTO());
        return "screens/customer/auth/login/index";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginDTO form,
                        BindingResult result,
                        HttpServletRequest request,
                        Model model) {
        if (result.hasErrors()) {
            return "screens/customer/auth/login/index";
        }

        LoginIdentityDTO account;
        try {
            account = authService.login(form.getEmail(), form.getPassword());
        } catch (AccountLockedException exception) {
            model.addAttribute("error", "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
            return "screens/customer/auth/login/index";
        }
        if (account == null) {
            model.addAttribute("error", "Email hoặc mật khẩu không đúng");
            return "screens/customer/auth/login/index";
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
        return "screens/customer/auth/register/index";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") CustomerRegisterDTO form,
                           BindingResult result,
                           Model model) {
        if (result.hasErrors()) {
            return "screens/customer/auth/register/index";
        }

        if (!customerService.register(form)) {
            model.addAttribute("error", "Email này đã được sử dụng");
            return "screens/customer/auth/register/index";
        }

        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
