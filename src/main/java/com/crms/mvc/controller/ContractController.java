package com.crms.mvc.controller;

import com.crms.mvc.service.ContractService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Serves issued contract PDFs. The files are not publicly mapped, so every download goes through an access check here.
 */
@Controller
public class ContractController {
    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/bookings/{id}/contract")
    public ResponseEntity<Resource> downloadOwnContract(@PathVariable("id") Long bookingId, HttpSession session) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return pdfResponse(bookingId, contractService.loadContractPdfForCustomer(bookingId, customerId));
    }

    @GetMapping("/admin/bookings/{id}/contract")
    public ResponseEntity<Resource> downloadContract(@PathVariable("id") Long bookingId) {
        return pdfResponse(bookingId, contractService.loadContractPdf(bookingId));
    }

    /** Lets staff retry when the automatic PDF generation or email failed. */
    @PostMapping("/admin/bookings/{id}/contract/issue")
    public String issueContract(@PathVariable("id") Long bookingId, RedirectAttributes ra) {
        try {
            contractService.issueContract(bookingId);
            ra.addFlashAttribute("success", "Contract issued. The customer email is sent when the mail server is reachable.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not issue contract: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }

    private ResponseEntity<Resource> pdfResponse(Long bookingId, Resource pdf) {
        ContentDisposition disposition = ContentDisposition.inline()
                .filename("contract-booking-" + bookingId + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf);
    }
}
