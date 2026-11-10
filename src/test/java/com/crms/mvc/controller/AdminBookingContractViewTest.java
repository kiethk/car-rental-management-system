package com.crms.mvc.controller;

import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.util.CurrentAccount;
import com.crms.mvc.support.TestSessions;
import org.springframework.context.annotation.Import;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.ContractDTO;
import com.crms.mvc.dto.IssueDTO;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.IssueType;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.ContractTemplateStatus;
import com.crms.mvc.dto.BookingSummaryDTO;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.IssueService;
import com.crms.mvc.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(AdminBookingController.class)
class AdminBookingContractViewTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private com.crms.mvc.service.BookingQueryService bookingQueryService;
    @MockitoBean private com.crms.mvc.service.VehicleAvailabilityService vehicleAvailabilityService;
    @MockitoBean private VehicleService vehicleService;
    @MockitoBean private com.crms.mvc.service.PaymentService paymentService;
    @MockitoBean private IssueService issueService;
    @MockitoBean private com.crms.mvc.service.HandoverService handoverService;

    @Test
    void confirmedBookingWithoutPdfOffersRetry() throws Exception {
        when(bookingQueryService.getBookingDetailForAdmin(42L)).thenReturn(confirmedBooking());
        when(vehicleService.findByStatusWithDetails("available")).thenReturn(List.of());

        MockHttpSession staff = TestSessions.staff(2L);

        mvc.perform(get("/admin/bookings/42").session(staff))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bookings/manage"))
                .andExpect(content().string(containsString("Điều khoản v1")))
                .andExpect(content().string(containsString("từ IP 127.0.0.1")))
                .andExpect(content().string(containsString("Phát hành PDF và gửi email")));
    }

    @Test
    void confirmedBookingWithRetiredVehicleWarnsAndOffersReassignment() throws Exception {
        BookingDetailDTO booking = confirmedBooking();
        booking.setVehicleId(9L);
        booking.setVehicleRetired(true);
        when(bookingQueryService.getBookingDetailForAdmin(42L)).thenReturn(booking);
        when(vehicleService.findByStatusWithDetails("available")).thenReturn(List.of());

        mvc.perform(get("/admin/bookings/42").session(staffSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Xe đã ngừng khai thác, cần gán lại")))
                .andExpect(content().string(containsString("Bước 1: Phân bổ xe vật lý")))
                .andExpect(content().string(not(containsString("Bước 2: Bàn giao xe cho khách"))));
    }

    @Test
    void confirmedBookingWithAWorkingVehicleShowsNoRetirementWarning() throws Exception {
        BookingDetailDTO booking = confirmedBooking();
        booking.setVehicleId(9L);
        when(bookingQueryService.getBookingDetailForAdmin(42L)).thenReturn(booking);
        when(vehicleService.findByStatusWithDetails("available")).thenReturn(List.of());

        mvc.perform(get("/admin/bookings/42").session(staffSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Xe đã ngừng khai thác"))))
                .andExpect(content().string(containsString("Bước 2: Bàn giao xe cho khách")));
    }

    @Test
    void anActiveBookingListsItsIssuesAndOffersToRecordOne() throws Exception {
        BookingDetailDTO booking = confirmedBooking();
        booking.setStatus("active");
        booking.setVehicleId(9L);
        when(bookingQueryService.getBookingDetailForAdmin(42L)).thenReturn(booking);
        when(vehicleService.findByStatusWithDetails("available")).thenReturn(List.of());
        IssueDTO issue = new IssueDTO();
        issue.setId(7L);
        issue.setIssueType(IssueType.DAMAGE);
        issue.setStatus(IssueStatus.OPEN);
        issue.setSubject("Scratch on the bumper");
        when(issueService.findByBooking(42L)).thenReturn(List.of(issue));

        mvc.perform(get("/admin/bookings/42").session(staffSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Scratch on the bumper")))
                .andExpect(content().string(containsString("/admin/issues/new?bookingId=42")));
    }

    @Test
    void theCheckOutFormHasNoHandTypedFineAndShowsTheConditionAtHandover() throws Exception {
        BookingDetailDTO booking = confirmedBooking();
        booking.setStatus("active");
        booking.setVehicleId(9L);
        when(bookingQueryService.getBookingDetailForAdmin(42L)).thenReturn(booking);
        when(vehicleService.findByStatusWithDetails("available")).thenReturn(List.of());

        mvc.perform(get("/admin/bookings/42").session(staffSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("additionalFine"))))
                .andExpect(content().string(containsString("Tình trạng lúc bàn giao")));
    }

    @Test
    void aBookingTheCustomerHasNotPickedUpHasNoIssueSection() throws Exception {
        when(bookingQueryService.getBookingDetailForAdmin(42L)).thenReturn(confirmedBooking());
        when(vehicleService.findByStatusWithDetails("available")).thenReturn(List.of());

        mvc.perform(get("/admin/bookings/42").session(staffSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/admin/issues/new?bookingId=42"))));
    }

    private static MockHttpSession staffSession() {
        return TestSessions.employee(2L, "staff", "Staff Member");
    }

    private BookingDetailDTO confirmedBooking() {
        OffsetDateTime now = OffsetDateTime.now();
        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setId(1L);
        terms.setVersionNo(1);
        terms.setTitle("Hợp đồng thuê xe ô tô tự lái");
        terms.setTermsContent("1. Bàn giao xe đúng hẹn.");
        terms.setDepositRate(new BigDecimal("0.5000"));
        terms.setLateFeeMultiplier(new BigDecimal("2.00"));
        terms.setFreeCancellationHours(72);
        terms.setStatus(ContractTemplateStatus.ACTIVE);

        ContractDTO contract = new ContractDTO();
        contract.setContractNumber("CTR-20261023-0042");
        contract.setAcceptedAt(now.minusHours(1));
        contract.setAcceptedIp("127.0.0.1");
        contract.setIssued(false);
        contract.setTerms(terms);

        BookingDetailDTO booking = new BookingDetailDTO();
        booking.setId(42L);
        booking.setCustomerName("Nguyễn Văn An");
        booking.setCustomerPhone("0901234567");
        booking.setCustomerEmail("an@example.com");
        booking.setVehicleTypeId(1L);
        booking.setVehicleTypeName("Vios");
        booking.setVehicleCategory("sedan");
        booking.setSeats(5);
        booking.setLicensePlate("Unassigned");
        booking.setPickupTime(now.plusDays(1));
        booking.setScheduledEndTime(now.plusDays(1).plusHours(9));
        booking.setPricePerHour(new BigDecimal("150000"));
        booking.setTotalPrice(new BigDecimal("1350000"));
        booking.setDepositAmount(new BigDecimal("675000"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setLateFee(BigDecimal.ZERO);
        booking.setStatus("confirmed");
        booking.setStaffId(2L);
        booking.setCreatedAt(now.minusHours(1));
        booking.setPayments(List.of());
        booking.setFees(List.of());
        booking.setContract(contract);
        return booking;
    }

    @Test
    void adminBookingsListShowsRefundTabAndAlert() throws Exception {
        when(paymentService.countPendingRefunds()).thenReturn(1L);

        BookingSummaryDTO summary = new BookingSummaryDTO();
        summary.setId(42L);
        summary.setCustomerName("Nguyen Van A");
        summary.setVehicleTypeName("Toyota Vios");
        summary.setStatus("cancelled");
        summary.setHasPendingRefund(true);
        summary.setPickupTime(OffsetDateTime.now().plusDays(2));
        when(bookingQueryService.getAllBookings("all")).thenReturn(List.of(summary));

        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute(CurrentAccount.SESSION_KEY,
                new SessionAccount(2L, SessionAccount.EMPLOYEE, "Staff Member", "staff@crms.test", "staff", null));

        mvc.perform(get("/admin/bookings").session(staff))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bookings/list"))
                .andExpect(content().string(containsString("Cần hoàn cọc")))
                .andExpect(content().string(containsString("Hoàn cọc ngay →")));
    }

    @Test
    void adminBookingsListFilterRefundQueue() throws Exception {
        when(paymentService.countPendingRefunds()).thenReturn(1L);

        BookingSummaryDTO summary = new BookingSummaryDTO();
        summary.setId(42L);
        summary.setCustomerName("Nguyen Van A");
        summary.setVehicleTypeName("Toyota Vios");
        summary.setStatus("cancelled");
        summary.setHasPendingRefund(true);
        summary.setPickupTime(OffsetDateTime.now().plusDays(2));
        when(bookingQueryService.getAllBookings("refund")).thenReturn(List.of(summary));

        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute(CurrentAccount.SESSION_KEY,
                new SessionAccount(2L, SessionAccount.EMPLOYEE, "Staff Member", "staff@crms.test", "staff", null));

        mvc.perform(get("/admin/bookings?status=refund").session(staff))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bookings/list"))
                .andExpect(content().string(containsString("Danh sách đơn chờ chuyển khoản hoàn tiền cọc")))
                .andExpect(content().string(containsString("Hoàn cọc ngay →")));
    }

    @Test
    void adminBookingsListFilterConfirmedStatus() throws Exception {
        when(paymentService.countPendingRefunds()).thenReturn(0L);

        BookingSummaryDTO summary = new BookingSummaryDTO();
        summary.setId(7L);
        summary.setCustomerName("Nguyen Van B");
        summary.setVehicleTypeName("Honda City");
        summary.setLicensePlate("51H-999.99");
        summary.setStatus("confirmed");
        summary.setHasPendingRefund(false);
        summary.setPickupTime(OffsetDateTime.now().plusDays(1));
        summary.setScheduledEndTime(OffsetDateTime.now().plusDays(2));
        when(bookingQueryService.getAllBookings("confirmed")).thenReturn(List.of(summary));

        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute(CurrentAccount.SESSION_KEY,
                new SessionAccount(2L, SessionAccount.EMPLOYEE, "Staff Member", "staff@crms.test", "staff", null));

        mvc.perform(get("/admin/bookings?status=confirmed").session(staff))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bookings/list"))
                .andExpect(content().string(containsString("Honda City")))
                .andExpect(content().string(containsString("Đã xác nhận")));
    }
}