package com.crms.mvc.controller;

import com.crms.mvc.dto.IssueActor;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.dto.TrafficFineFormDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.service.BookingQueryService;
import com.crms.mvc.service.TrafficFineService;
import com.crms.mvc.util.CurrentAccount;
import com.crms.mvc.util.MediaUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Traffic fines (phạt nguội): staff look up the rental from the plate and time printed on the police notice, record
 * the fine with a photo of the notice, and the customer is emailed. Nothing is charged: the police contact the
 * customer directly.
 */
@Controller
@RequestMapping("/admin/traffic-fines")
public class AdminTrafficFineController {

    private static final String LIST_PATH = "/admin/traffic-fines";

    private final TrafficFineService trafficFineService;
    private final BookingQueryService bookingQueryService;

    public AdminTrafficFineController(TrafficFineService trafficFineService, BookingQueryService bookingQueryService) {
        this.trafficFineService = trafficFineService;
        this.bookingQueryService = bookingQueryService;
    }

    /** The list, plus the lookup. A lookup that finds a rental shows it with the form to record the fine. */
    @GetMapping
    public String list(@RequestParam(value = "plate", required = false) String plate,
                       @RequestParam(value = "violationAt", required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime violationAt,
                       Model model) {
        if (plate != null && !plate.isBlank() && violationAt != null) {
            try {
                Booking rental = trafficFineService.findBookingForViolation(plate, violationAt);
                TrafficFineFormDTO form = new TrafficFineFormDTO();
                form.setBookingId(rental.getId());
                form.setPlate(plate.trim().toUpperCase());
                form.setViolationAt(violationAt);
                model.addAttribute("fineForm", form);
                model.addAttribute("found", bookingQueryService.getBookingDetailForAdmin(rental.getId()));
            } catch (NoSuchElementException ex) {
                model.addAttribute("lookupError", ex.getMessage());
            }
        }
        return listPage(model, plate, violationAt);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("fineForm") TrafficFineFormDTO form,
                         BindingResult result,
                         HttpSession session,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            showFound(model, form.getBookingId());
            return listPage(model, form.getPlate(), form.getViolationAt());
        }
        try {
            trafficFineService.record(form, actorOf(session).employeeId());
            ra.addFlashAttribute("success", "Đã ghi nhận phạt nguội và gửi email thông báo cho khách hàng.");
            return "redirect:" + LIST_PATH;
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            showFound(model, form.getBookingId());
            return listPage(model, form.getPlate(), form.getViolationAt());
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            trafficFineService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa bản ghi và gửi email thông báo cho khách hàng.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + LIST_PATH;
    }

    /** The police notice is private evidence, so it is served here (behind the staff login), never from a public folder. */
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Long id) {
        Resource image = trafficFineService.loadImage(id);
        return ResponseEntity.ok().contentType(MediaUtils.resolveMediaType(image.getFilename())).body(image);
    }

    private String listPage(Model model, String plate, LocalDateTime violationAt) {
        model.addAttribute("notices", trafficFineService.findAll());
        model.addAttribute("lookupPlate", plate);
        model.addAttribute("lookupAt", violationAt);
        model.addAttribute("activeNav", "traffic-fines");
        return "admin/traffic-fines/list";
    }

    private void showFound(Model model, Long bookingId) {
        if (bookingId != null) {
            try {
                model.addAttribute("found", bookingQueryService.getBookingDetailForAdmin(bookingId));
            } catch (RuntimeException ignored) {
                // the form is shown without the rental card; the error above already explains
            }
        }
    }

    private IssueActor actorOf(HttpSession session) {
        // RoleInterceptor already guarantees a logged-in employee on /admin/**.
        return CurrentAccount.of(session)
                .filter(SessionAccount::isEmployee)
                .map(IssueActor::of)
                .orElseThrow(() -> new IllegalStateException("Cần đăng nhập bằng tài khoản nhân viên"));
    }
}
