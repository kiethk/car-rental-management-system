package com.crms.mvc.controller;

import com.crms.mvc.dto.AccountProfileDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.service.AccountService;
import com.crms.mvc.util.CurrentAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.NoSuchElementException;
import java.util.Optional;

@Controller
public class AccountController {
    private static final String CUSTOMER_VIEW = "screens/customer/profile/index/index";
    private static final String EMPLOYEE_VIEW = "screens/admin/profile/index";

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @InitBinder("profileForm")
    public void allowEditableFields(WebDataBinder binder) {
        binder.setAllowedFields("phone", "currentPassword", "newPassword", "confirmPassword");
    }

    @GetMapping("/profile")
    public String customerProfile(HttpSession session, Model model) {
        Optional<SessionAccount> account = CurrentAccount.of(session).filter(SessionAccount::isCustomer);
        if (account.isEmpty()) {
            return "redirect:/login";
        }
        return showForm(account.get(), session, model, CUSTOMER_VIEW);
    }

    @PostMapping("/profile")
    public String updateCustomerProfile(@Valid @ModelAttribute("profileForm") AccountProfileDTO form,
                                        BindingResult result,
                                        HttpServletRequest request,
                                        Model model) {
        Optional<SessionAccount> account = CurrentAccount.of(request.getSession(false))
                .filter(SessionAccount::isCustomer);
        if (account.isEmpty()) {
            return "redirect:/login";
        }
        return saveForm(account.get(), form, result, request, model, CUSTOMER_VIEW, "/profile");
    }

    @GetMapping("/admin/profile")
    public String employeeProfile(HttpSession session, Model model) {
        Optional<SessionAccount> account = CurrentAccount.of(session).filter(SessionAccount::isEmployee);
        if (account.isEmpty()) {
            return "redirect:/login";
        }
        return showForm(account.get(), session, model, EMPLOYEE_VIEW);
    }

    @PostMapping("/admin/profile")
    public String updateEmployeeProfile(@Valid @ModelAttribute("profileForm") AccountProfileDTO form,
                                        BindingResult result,
                                        HttpServletRequest request,
                                        Model model) {
        Optional<SessionAccount> account = CurrentAccount.of(request.getSession(false))
                .filter(SessionAccount::isEmployee);
        if (account.isEmpty()) {
            return "redirect:/login";
        }
        return saveForm(account.get(), form, result, request, model, EMPLOYEE_VIEW, "/admin/profile");
    }

    private String showForm(SessionAccount account, HttpSession session, Model model, String view) {
        try {
            model.addAttribute("profileForm", accountService.getProfile(account));
            return view;
        } catch (NoSuchElementException exception) {
            session.invalidate();
            return "redirect:/login";
        }
    }

    private String saveForm(SessionAccount account,
                            AccountProfileDTO form,
                            BindingResult result,
                            HttpServletRequest request,
                            Model model,
                            String view,
                            String successPath) {
        try {
            if (result.hasErrors()) {
                fillReadOnlyFields(account, form);
                return view;
            }

            SessionAccount updated = accountService.update(account, form);
            if (form.isPasswordChangeRequested()) {
                request.changeSessionId();
            }
            request.getSession().setAttribute(CurrentAccount.SESSION_KEY, updated);
            return "redirect:" + successPath + "?updated";
        } catch (IllegalArgumentException exception) {
            result.rejectValue("currentPassword", "currentPassword.incorrect", exception.getMessage());
            fillReadOnlyFields(account, form);
            return view;
        } catch (NoSuchElementException exception) {
            request.getSession().invalidate();
            return "redirect:/login";
        } catch (DataAccessException exception) {
            model.addAttribute("error", "Không thể cập nhật tài khoản. Vui lòng thử lại sau.");
            return view;
        }
    }

    private void fillReadOnlyFields(SessionAccount account, AccountProfileDTO form) {
        AccountProfileDTO profile = accountService.getProfile(account);
        form.setName(profile.getName());
        form.setEmail(profile.getEmail());
    }
}
