package com.crms.mvc.controller;

import com.crms.mvc.util.CurrentAccount;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.ContractTemplateFormDTO;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.util.EmployeeRole;
import jakarta.servlet.http.HttpSession;
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

/**
 * Rental terms versions. Staff may read them; only admins may draft, edit, activate or delete.
 */
@Controller
@RequestMapping("/admin/contract-templates")
public class AdminContractTemplateController {
    private static final String ADMIN_ONLY_MESSAGE = "Chỉ quản trị viên mới được thay đổi điều khoản thuê xe.";

    private final ContractTemplateService contractTemplateService;

    public AdminContractTemplateController(ContractTemplateService contractTemplateService) {
        this.contractTemplateService = contractTemplateService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        model.addAttribute("templates", contractTemplateService.getAllTemplates());
        model.addAttribute("isAdmin", isAdmin(session));
        return "admin/contract-templates/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        ContractTemplateDTO template = contractTemplateService.getTemplate(id);
        if (!model.containsAttribute("templateForm")) {
            model.addAttribute("templateForm", ContractTemplateFormDTO.fromTemplate(template));
        }
        model.addAttribute("template", template);
        model.addAttribute("isAdmin", isAdmin(session));
        return "admin/contract-templates/detail";
    }

    @PostMapping("/draft")
    public String createDraft(HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", ADMIN_ONLY_MESSAGE);
            return "redirect:/admin/contract-templates";
        }
        try {
            ContractTemplateDTO draft = contractTemplateService.createDraft(CurrentAccount.employeeId(session).orElse(null));
            ra.addFlashAttribute("success", "Đã tạo bản nháp phiên bản " + draft.getVersionNo() + " từ điều khoản đang hiệu lực.");
            return "redirect:/admin/contract-templates/" + draft.getId();
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/contract-templates";
        }
    }

    @PostMapping("/{id}")
    public String updateDraft(@PathVariable Long id,
                              @Valid @ModelAttribute("templateForm") ContractTemplateFormDTO form,
                              BindingResult result,
                              HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", ADMIN_ONLY_MESSAGE);
            return "redirect:/admin/contract-templates/" + id;
        }
        if (result.hasErrors()) {
            model.addAttribute("template", contractTemplateService.getTemplate(id));
            model.addAttribute("isAdmin", true);
            return "admin/contract-templates/detail";
        }
        try {
            contractTemplateService.updateDraft(id, form);
            ra.addFlashAttribute("success", "Đã lưu bản nháp. Hãy kích hoạt khi sẵn sàng áp dụng cho đơn thuê mới.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/contract-templates/" + id;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", ADMIN_ONLY_MESSAGE);
            return "redirect:/admin/contract-templates/" + id;
        }
        try {
            contractTemplateService.activate(id);
            ra.addFlashAttribute("success", "Điều khoản mới đã có hiệu lực. Các đơn đã đặt vẫn giữ phiên bản khách đã đồng ý.");
            return "redirect:/admin/contract-templates";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/contract-templates/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteDraft(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", ADMIN_ONLY_MESSAGE);
            return "redirect:/admin/contract-templates/" + id;
        }
        try {
            contractTemplateService.deleteDraft(id);
            ra.addFlashAttribute("success", "Đã xóa bản nháp.");
            return "redirect:/admin/contract-templates";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/contract-templates/" + id;
        }
    }

    private boolean isAdmin(HttpSession session) {
        return CurrentAccount.isAdmin(session);
    }
}
