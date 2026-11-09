package com.crms.mvc.controller;

import com.crms.mvc.dto.AccountQueryDTO;
import com.crms.mvc.dto.AccountSummaryDTO;
import com.crms.mvc.dto.EmployeeCreateDTO;
import com.crms.mvc.dto.PageNavDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.service.AccountManagementService;
import com.crms.mvc.util.CurrentAccount;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/accounts")
public class AdminAccountController {
    private static final String LIST_PATH = "/admin/accounts";

    private final AccountManagementService accountManagementService;

    public AdminAccountController(AccountManagementService accountManagementService) {
        this.accountManagementService = accountManagementService;
    }

    @GetMapping
    public String list(@ModelAttribute("query") AccountQueryDTO query,
                       BindingResult ignoredBindingErrors,
                       HttpSession session,
                       Model model) {
        requireAdmin(session);
        Page<AccountSummaryDTO> page = accountManagementService.search(query);
        query.setPage(page.getNumber() + 1);

        model.addAttribute("accounts", page.getContent());
        model.addAttribute("nav", PageNavDTO.of(page, number -> query.urlForPage(LIST_PATH, number)));
        model.addAttribute("currentAccountId", CurrentAccount.of(session).map(SessionAccount::getId).orElse(null));
        return "screens/admin/accounts/list/index";
    }

    @PostMapping("/{type}/{id}/status")
    public String updateStatus(@PathVariable("type") String type,
                               @PathVariable("id") Long id,
                               @RequestParam("active") boolean active,
                               HttpSession session,
                               RedirectAttributes ra) {
        SessionAccount actor = requireAdmin(session);
        try {
            accountManagementService.setActive(type, id, active, actor);
            ra.addFlashAttribute("success", active ? "Đã kích hoạt tài khoản." : "Đã khóa tài khoản.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        ra.addAttribute("type", type);
        return "redirect:" + LIST_PATH;
    }

    @GetMapping("/employees/new")
    public String employeeForm(HttpSession session, Model model) {
        requireAdmin(session);
        model.addAttribute("employeeForm", new EmployeeCreateDTO());
        return "screens/admin/accounts/form/index";
    }

    @PostMapping("/employees")
    public String createEmployee(@Valid @ModelAttribute("employeeForm") EmployeeCreateDTO form,
                                 BindingResult result,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes ra) {
        requireAdmin(session);
        if (result.hasErrors()) {
            return "screens/admin/accounts/form/index";
        }
        try {
            accountManagementService.createEmployee(form);
        } catch (IllegalArgumentException | DataIntegrityViolationException e) {
            model.addAttribute("error", "Email này đã được sử dụng");
            return "screens/admin/accounts/form/index";
        }
        ra.addFlashAttribute("success", "Đã tạo tài khoản nhân viên.");
        ra.addAttribute("type", SessionAccount.EMPLOYEE);
        return "redirect:" + LIST_PATH;
    }

    private SessionAccount requireAdmin(HttpSession session) {
        return CurrentAccount.of(session)
                .filter(SessionAccount::isAdmin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Cần quyền quản trị viên"));
    }
}
