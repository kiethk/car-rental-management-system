package com.crms.mvc.controller;

import com.crms.mvc.dto.LicenseSubmitDTO;
import com.crms.mvc.entity.LicenseDocument;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.LicenseDocumentService;
import com.crms.mvc.util.CurrentAccount;
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

import java.time.LocalDate;
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
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) {
            return "redirect:/login";
        }

        addCurrentLicense(customerId, model);
        model.addAttribute("licenseForm", new LicenseSubmitDTO());
        return "licenses/upload";
    }

    @PostMapping("/licenses")
    public String submitLicense(@Valid @ModelAttribute("licenseForm") LicenseSubmitDTO form,
                                BindingResult result,
                                HttpSession session,
                                Model model) {
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) {
            return "redirect:/login";
        }

        if (form.getLicenseImage() == null || form.getLicenseImage().isEmpty()) {
            result.rejectValue("licenseImage", "required", "Vui lòng chọn ảnh mặt trước bằng lái xe (License photo is required)");
        }

        if (result.hasErrors()) {
            addCurrentLicense(customerId, model);
            return "licenses/upload";
        }

        try {
            if (!licenseDocumentService.submitLicense(customerId, form)) {
                session.invalidate();
                return "redirect:/login";
            }
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            addCurrentLicense(customerId, model);
            return "licenses/upload";
        } catch (IllegalStateException exception) {
            model.addAttribute("error", "Could not save the license image.");
            addCurrentLicense(customerId, model);
            return "licenses/upload";
        }

        return "redirect:/licenses/my-license?submitted";
    }

    @GetMapping("/licenses/{licenseId}/image")
    public ResponseEntity<Resource> licenseImage(@PathVariable Long licenseId,
                                                 HttpSession session) {
        LicenseDocument license = licenseDocumentService.getLicense(licenseId);
        if (license == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isCustomerOwner = CurrentAccount.customerId(session)
                .map(id -> id.equals(license.getCustomer().getId()))
                .orElse(false);
        if (!isCustomerOwner && !CurrentAccount.isEmployee(session)) {
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

    private void addCurrentLicense(Long customerId, Model model) {
        LicenseDocument license = licenseDocumentService.getCustomerLicense(customerId);
        model.addAttribute("currentLicense", license);
        model.addAttribute("licenseExpired",
                license != null && license.getLicenseExpiry().isBefore(LocalDate.now()));
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
