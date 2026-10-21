package com.crms.mvc.controller;

import com.crms.mvc.dto.LicenseApprovalDTO;
import com.crms.mvc.dto.LicenseSubmitDTO;
import com.crms.mvc.entity.LicenseDocument;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.LicenseDocumentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Locale;

@Controller
public class LicenseDocumentController {
    private final LicenseDocumentService licenseDocumentService;
    private final FileUploadService fileUploadService;

    public LicenseDocumentController(LicenseDocumentService licenseDocumentService,
                                     FileUploadService fileUploadService) {
        this.licenseDocumentService = licenseDocumentService;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping("/licenses/my-license")
    public String customerLicense(HttpSession session, Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/admin/licenses";
        }

        model.addAttribute("currentLicense",
                licenseDocumentService.getCustomerLicense(customerId));
        model.addAttribute("licenseForm", new LicenseSubmitDTO());
        return "licenses/upload";
    }

    @PostMapping("/licenses")
    public String submitLicense(@Valid @ModelAttribute("licenseForm") LicenseSubmitDTO form,
                                BindingResult result,
                                HttpSession session,
                                Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/admin/licenses";
        }

        if (result.hasErrors()) {
            model.addAttribute("currentLicense",
                    licenseDocumentService.getCustomerLicense(customerId));
            return "licenses/upload";
        }

        try {
            if (!licenseDocumentService.submitLicense(customerId, form)) {
                model.addAttribute("error",
                        "You already have a pending or approved license.");
                model.addAttribute("currentLicense",
                        licenseDocumentService.getCustomerLicense(customerId));
                return "licenses/upload";
            }
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("currentLicense",
                    licenseDocumentService.getCustomerLicense(customerId));
            return "licenses/upload";
        } catch (IllegalStateException exception) {
            model.addAttribute("error", "Could not save the license image.");
            model.addAttribute("currentLicense",
                    licenseDocumentService.getCustomerLicense(customerId));
            return "licenses/upload";
        }

        return "redirect:/licenses/my-license?submitted";
    }

    @GetMapping("/admin/licenses")
    public String pendingLicenses(Model model) {
        model.addAttribute("licenses", licenseDocumentService.getPendingLicenses());
        return "admin/licenses/list";
    }

    @GetMapping("/admin/licenses/{licenseId}")
    public String licenseDetail(@PathVariable Long licenseId, Model model) {
        LicenseDocument license = licenseDocumentService.getLicense(licenseId);
        if (license == null) {
            return "redirect:/admin/licenses";
        }

        model.addAttribute("license", license);
        model.addAttribute("approvalForm", new LicenseApprovalDTO());
        return "admin/licenses/detail";
    }

    @PostMapping("/admin/licenses/{licenseId}/review")
    public String reviewLicense(@PathVariable Long licenseId,
                                @Valid @ModelAttribute("approvalForm") LicenseApprovalDTO form,
                                BindingResult result,
                                HttpSession session,
                                Model model) {
        LicenseDocument license = licenseDocumentService.getLicense(licenseId);
        if (license == null) {
            return "redirect:/admin/licenses";
        }

        if (result.hasErrors()) {
            model.addAttribute("license", license);
            return "admin/licenses/detail";
        }

        Long employeeId = (Long) session.getAttribute("employeeId");
        if (!licenseDocumentService.reviewLicense(licenseId, employeeId, form)) {
            model.addAttribute("error", "Could not review this license.");
            model.addAttribute("license", license);
            return "admin/licenses/detail";
        }

        return "redirect:/admin/licenses";
    }

    @GetMapping("/licenses/{licenseId}/image")
    public ResponseEntity<Resource> licenseImage(@PathVariable Long licenseId,
                                                 HttpSession session) {
        LicenseDocument license = licenseDocumentService.getLicense(licenseId);
        if (license == null) {
            return ResponseEntity.notFound().build();
        }

        Long customerId = (Long) session.getAttribute("customerId");
        Long employeeId = (Long) session.getAttribute("employeeId");
        String employeeRole = (String) session.getAttribute("employeeRole");

        boolean isCustomerOwner = customerId != null
                && customerId.equals(license.getCustomer().getId());
        boolean isAllowedEmployee = employeeId != null
                && ("admin".equals(employeeRole) || "staff".equals(employeeRole));

        if (!isCustomerOwner && !isAllowedEmployee) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String fileName = license.getLicenseImageUrl();
        Resource image = fileUploadService.loadLicenseImage(fileName);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(getImageMediaType(fileName))
                .body(image);
    }

    private MediaType getImageMediaType(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase(Locale.ROOT);
        if (lowerCaseFileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lowerCaseFileName.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
