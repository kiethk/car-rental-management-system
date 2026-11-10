package com.crms.mvc.controller;

import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.IssueActor;
import com.crms.mvc.dto.IssueDTO;
import com.crms.mvc.dto.IssueFormDTO;
import com.crms.mvc.dto.IssueQueryDTO;
import com.crms.mvc.dto.PageNavDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.entity.Issue;
import com.crms.mvc.enums.IssueType;
import com.crms.mvc.service.BookingQueryService;
import com.crms.mvc.service.IssueService;
import com.crms.mvc.util.DateTimeUtils;
import com.crms.mvc.util.CurrentAccount;
import com.crms.mvc.util.MediaUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin/issues")
public class AdminIssueController {

    private static final String LIST_PATH = "/admin/issues";

    private final IssueService issueService;
    private final BookingQueryService bookingQueryService;

    public AdminIssueController(IssueService issueService, BookingQueryService bookingQueryService) {
        this.issueService = issueService;
        this.bookingQueryService = bookingQueryService;
    }

    @GetMapping
    public String list(@ModelAttribute("query") IssueQueryDTO query,
                       BindingResult ignoredBindingErrors,
                       Model model) {
        Page<IssueDTO> page = issueService.searchForAdmin(query);
        query.setPage(page.getNumber() + 1);

        model.addAttribute("issues", page.getContent());
        model.addAttribute("nav", PageNavDTO.of(page, number -> query.urlForPage(LIST_PATH, number)));
        model.addAttribute("openCount", issueService.countOpen());
        model.addAttribute("issueTypes", IssueType.values());
        model.addAttribute("activeNav", "issues");
        return "screens/admin/issues/list/index";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam("bookingId") Long bookingId,
                          @RequestParam(value = "issueType", required = false) IssueType issueType,
                          @RequestParam(value = "occurredAt", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredAt,
                          Model model) {
        if (!model.containsAttribute("issueForm")) {
            IssueFormDTO form = new IssueFormDTO();
            form.setBookingId(bookingId);
            form.setIssueType(issueType != null ? issueType : IssueType.DAMAGE);
            form.setOccurredAt(occurredAt != null ? occurredAt
                    : LocalDateTime.now(DateTimeUtils.VIETNAM_ZONE).truncatedTo(ChronoUnit.MINUTES));
            model.addAttribute("issueForm", form);
        }
        return formPage(model, bookingId, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("issueForm") IssueFormDTO form,
                         BindingResult result,
                         HttpSession session,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            return formPage(model, form.getBookingId(), null);
        }
        try {
            Issue created = issueService.create(form, actorOf(session));
            ra.addFlashAttribute("success", "Đã ghi nhận sự cố #" + created.getId() + " và gửi email thông báo cho khách.");
            return "redirect:" + LIST_PATH + "/" + created.getId();
        } catch (IllegalStateException | IllegalArgumentException | NoSuchElementException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            // The uploaded files belong to this request only; staff pick the photos again.
            form.setImages(new ArrayList<>());
            ra.addFlashAttribute("issueForm", form);
            return "redirect:" + LIST_PATH + "/new?bookingId=" + form.getBookingId();
        }
    }

    @PostMapping("/{id}/settle-direct")
    public String settleDirectly(@PathVariable Long id,
                                 @RequestParam(value = "paymentMethod", defaultValue = "CASH") String paymentMethod,
                                 @RequestParam(value = "note", defaultValue = "") String note,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        return act(id, ra, "Đã ghi nhận thu tiền tại quầy cho sự cố #" + id + ".",
                () -> issueService.settleDirectly(id, paymentMethod, note, actorOf(session)));
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(value = "reason", defaultValue = "") String reason,
                         HttpSession session,
                         RedirectAttributes ra) {
        return act(id, ra, "Đã huỷ sự cố #" + id + ".", () -> issueService.cancel(id, reason, actorOf(session)));
    }

    @PostMapping("/{id}/evidences")
    public String addPhoto(@PathVariable Long id, @RequestParam("image") MultipartFile image, RedirectAttributes ra) {
        return act(id, ra, "Đã thêm ảnh bằng chứng.", () -> issueService.addEvidence(id, image));
    }

    @PostMapping("/{id}/evidences/{evidenceId}/delete")
    public String removePhoto(@PathVariable Long id, @PathVariable Long evidenceId, RedirectAttributes ra) {
        return act(id, ra, "Đã xoá ảnh.", () -> issueService.removeEvidence(id, evidenceId));
    }

    /** Photos are private evidence, so they are served here (behind the staff login) and never from a public folder. */
    @GetMapping("/{id}/evidences/{evidenceId}")
    public ResponseEntity<Resource> photo(@PathVariable Long id, @PathVariable Long evidenceId) {
        Resource photo = issueService.loadEvidence(id, evidenceId);
        return ResponseEntity.ok().contentType(MediaUtils.resolveMediaType(photo.getFilename())).body(photo);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        IssueDTO issue = issueService.findDetail(id).orElse(null);
        if (issue == null) {
            ra.addFlashAttribute("error", "Không tìm thấy sự cố: " + id);
            return "redirect:" + LIST_PATH;
        }
        model.addAttribute("issue", issue);
        model.addAttribute("activeNav", "issues");
        return "screens/admin/issues/detail/index";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        IssueDTO issue = issueService.findDetail(id).orElse(null);
        if (issue == null) {
            ra.addFlashAttribute("error", "Không tìm thấy sự cố: " + id);
            return "redirect:" + LIST_PATH;
        }
        if (!model.containsAttribute("issueForm")) {
            model.addAttribute("issueForm", toForm(issue));
        }
        return formPage(model, issue.getBookingId(), id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("issueForm") IssueFormDTO form,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            return formPage(model, form.getBookingId(), id);
        }
        try {
            issueService.update(id, form);
            ra.addFlashAttribute("success", "Đã cập nhật sự cố #" + id + ".");
            return "redirect:" + LIST_PATH + "/" + id;
        } catch (NoSuchElementException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:" + LIST_PATH;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("issueForm", form);
            return "redirect:" + LIST_PATH + "/" + id + "/edit";
        }
    }

    /** Runs one lifecycle step; a rule the service enforces comes back as a message on the issue page. */
    private String act(Long id, RedirectAttributes ra, String successMessage, Runnable step) {
        try {
            step.run();
            ra.addFlashAttribute("success", successMessage);
            return "redirect:" + LIST_PATH + "/" + id;
        } catch (NoSuchElementException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:" + LIST_PATH;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:" + LIST_PATH + "/" + id;
        }
    }

    private String formPage(Model model, Long bookingId, Long issueId) {
        BookingDetailDTO booking = bookingQueryService.getBookingDetailForAdmin(bookingId);
        model.addAttribute("booking", booking);
        model.addAttribute("issueId", issueId);
        model.addAttribute("issueTypes", IssueType.values());
        model.addAttribute("activeNav", "issues");
        return "screens/admin/issues/form/index";
    }

    private IssueActor actorOf(HttpSession session) {
        // RoleInterceptor already guarantees a logged-in employee on /admin/**.
        return CurrentAccount.of(session)
                .filter(SessionAccount::isEmployee)
                .map(IssueActor::of)
                .orElseThrow(() -> new IllegalStateException("Cần đăng nhập bằng tài khoản nhân viên"));
    }

    private IssueFormDTO toForm(IssueDTO issue) {
        IssueFormDTO form = new IssueFormDTO();
        form.setBookingId(issue.getBookingId());
        form.setIssueType(issue.getIssueType());
        form.setSubject(issue.getSubject());
        form.setDescription(issue.getDescription());
        form.setOccurredAt(issue.getOccurredAt().toLocalDateTime());
        form.setFeeAmount(issue.getFeeAmount());
        return form;
    }
}
