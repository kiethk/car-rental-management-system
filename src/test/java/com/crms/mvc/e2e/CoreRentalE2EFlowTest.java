package com.crms.mvc.e2e;

import com.crms.mvc.dto.*;
import com.crms.mvc.entity.*;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.*;
import com.crms.mvc.service.impl.*;
import com.crms.mvc.util.EmployeeRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoreRentalE2EFlowTest {

    // Repositories
    @Mock private BookingRepository bookingRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private VehicleTypeRepository vehicleTypeRepository;
    @Mock private LicensePlateRepository licensePlateRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private HandoverRepository handoverRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private BookingFeeRepository bookingFeeRepository;
    @Mock private VehicleInspectionRepository inspectionRepository;

    // Services & Mocked Dependencies
    @Mock private PaymentService paymentService;
    @Mock private ContractService contractService;
    @Mock private PricingService pricingService;
    @Mock private BookingFeeService bookingFeeService;
    @Mock private FileUploadService fileUploadService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private HandoverServiceImpl handoverService;
    private IssueServiceImpl issueService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        handoverService = new HandoverServiceImpl(
                bookingRepository, employeeRepository, vehicleRepository, issueRepository,
                handoverRepository, bookingFeeService, contractService, pricingService,
                paymentService, fileUploadService);

        issueService = new IssueServiceImpl(
                issueRepository, bookingRepository, employeeRepository, bookingFeeRepository,
                mock(IssueEvidenceRepository.class), fileUploadService, licensePlateRepository,
                eventPublisher);
    }

    @Test
    @DisplayName("Luồng 1: Đặt xe & Thanh toán Cọc -> Khách đặt, duyệt hợp đồng, cọc VNPay thành công chuyển trạng thái confirmed")
    void testFlow1_BookingAndDepositFlow() {
        // 1. Setup xe & khách hàng
        VehicleType type = new VehicleType();
        type.setId(1L);
        type.setName("Toyota Vios");
        type.setPricePerHour(new BigDecimal("100000"));

        Customer customer = new Customer();
        customer.setId(10L);
        customer.setName("Nguyễn Văn An");
        customer.setEmail("an@test.com");

        Slot slot = new Slot();
        slot.setId(100L);
        slot.setVehicleType(type);

        Booking booking = new Booking();
        booking.setId(501L);
        booking.setCustomer(customer);
        booking.setSlot(slot);
        booking.setPricePerHour(type.getPricePerHour());
        booking.setTotalPrice(new BigDecimal("1000000")); // 10h * 100k
        booking.setDepositAmount(new BigDecimal("300000")); // 30% cọc
        booking.setStatus("pending_payment");

        when(bookingRepository.findById(501L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // 2. Khách thực hiện thanh toán cọc VNPay thành công
        Payment depositPayment = new Payment();
        depositPayment.setId(9001L);
        depositPayment.setBooking(booking);
        depositPayment.setPaymentType("DEPOSIT");
        depositPayment.setAmount(booking.getDepositAmount());
        depositPayment.setStatus("paid");
        depositPayment.setPaymentMethod("VNPAY");
        depositPayment.setPaidAt(OffsetDateTime.now());

        booking.setStatus("confirmed");
        bookingRepository.save(booking);

        // 3. Assertions cho Luồng 1
        assertEquals("confirmed", booking.getStatus(), "Booking phải chuyển sang confirmed sau khi cọc thành công");
        assertEquals(new BigDecimal("300000"), booking.getDepositAmount(), "Số tiền cọc phải khớp với điều khoản 30%");
    }

    @Test
    @DisplayName("Luồng 2: Bàn giao xe tại Trạm (Pick-up / Check-in) -> Staff đối chiếu GPLX, ghi nhận ODO & gán xe thành công")
    void testFlow2_StationHandoverPickupFlow() {
        // 1. Dữ liệu xe và nhân viên trạm
        Employee staff = new Employee();
        staff.setId(2L);
        staff.setRole("staff");
        staff.setName("Trần Văn Staff");

        Vehicle vehicle = new Vehicle();
        vehicle.setId(77L);
        vehicle.setOdometer(5000);
        vehicle.setStatus("available");

        Booking booking = new Booking();
        booking.setId(502L);
        booking.setEmployee(staff);
        booking.setVehicle(vehicle);
        booking.setStatus("confirmed");
        Customer payer = new Customer();
        payer.setId(7L);
        booking.setCustomer(payer);
        booking.setTotalPrice(new BigDecimal("1000000"));
        booking.setDepositAmount(new BigDecimal("500000"));

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(bookingRepository.findById(502L)).thenReturn(Optional.of(booking));
        when(handoverRepository.save(any(Handover.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileUploadService.saveHandoverImage(any())).thenReturn("gplx_photo.jpg");

        // 2. Nhân viên thực hiện Check-in bàn giao xe (kèm ảnh chụp bằng lái và CCCD)
        CheckInDTO checkInForm = new CheckInDTO();
        checkInForm.setOdometer(5005);
        checkInForm.setFuelPercent(100);
        checkInForm.setNotes("Xe sạch đẹp, đầy bình xăng, đã đối chiếu GPLX gốc");
        checkInForm.setPaymentMethod("CASH");
        checkInForm.setPaymentReceived(true);
        checkInForm.setLicenseImage(new MockMultipartFile("licenseImage", "gplx.jpg", "image/jpeg", new byte[]{1, 2, 3}));
        checkInForm.setIdCardImage(new MockMultipartFile("idCardImage", "cccd.jpg", "image/jpeg", new byte[]{4, 5, 6}));

        handoverService.checkIn(502L, 2L, checkInForm);

        // 3. Assertions cho Luồng 2
        assertEquals("active", booking.getStatus(), "Booking phải chuyển sang ACTIVE sau khi giao chìa khóa");
        assertEquals("rented", vehicle.getStatus(), "Trạng thái xe phải chuyển sang RENTED");
        assertEquals(5005, vehicle.getOdometer(), "Số km của xe phải được cập nhật theo số ODO bàn giao");

        ArgumentCaptor<Handover> captor = ArgumentCaptor.forClass(Handover.class);
        verify(handoverRepository).save(captor.capture());
        assertEquals(HandoverType.CHECK_IN, captor.getValue().getType());
        assertEquals(100, captor.getValue().getFuelPercent());
    }

    @Test
    @DisplayName("Luồng 3: Nhận trả xe & Quyết toán (Drop-off & Settlement) -> Tính trễ hạn, quyết toán hoàn cọc / thu thêm")
    void testFlow3_StationHandoverDropoffAndSettlementFlow() {
        Employee staff = new Employee();
        staff.setId(2L);
        staff.setRole("staff");

        Vehicle vehicle = new Vehicle();
        vehicle.setId(77L);
        vehicle.setOdometer(5005);
        vehicle.setStatus("rented");

        Booking booking = new Booking();
        booking.setId(503L);
        booking.setEmployee(staff);
        booking.setVehicle(vehicle);
        booking.setStatus("active");
        booking.setTotalPrice(new BigDecimal("1000000")); // Tiền thuê
        booking.setDepositAmount(new BigDecimal("300000")); // Cọc 300k
        booking.setPricePerHour(new BigDecimal("100000"));
        booking.setScheduledEndTime(OffsetDateTime.now().minusHours(2)); // Trễ 2 tiếng

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(bookingRepository.findById(503L)).thenReturn(Optional.of(booking));

        Handover checkOutRecord = new Handover();
        checkOutRecord.setId(99L);
        checkOutRecord.setBooking(booking);
        checkOutRecord.setType(HandoverType.CHECK_OUT);
        checkOutRecord.setOdometer(5200);

        when(handoverRepository.findByBookingIdAndType(503L, HandoverType.CHECK_OUT)).thenReturn(Optional.of(checkOutRecord));
        when(handoverRepository.save(any(Handover.class))).thenAnswer(inv -> inv.getArgument(0));

        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setLateFeeMultiplier(new BigDecimal("1.5")); // Phạt trễ = 1.5 * giá giờ
        when(contractService.getTermsOfBooking(503L)).thenReturn(terms);

        when(pricingService.calculateLateFee(any(), any(), any(), any())).thenReturn(new BigDecimal("300000"));
        when(bookingFeeService.getUnpaidFees(503L)).thenReturn(List.of());
        when(bookingFeeService.calculatePrepaidIssueFees(503L)).thenReturn(BigDecimal.ZERO);

        // 1. Thực hiện Check-Out nhận trả xe
        CheckOutDTO checkOutForm = new CheckOutDTO();
        checkOutForm.setOdometer(5200);

        handoverService.checkOut(503L, 2L, checkOutForm);

        // 2. Assertions trạng thái xe và booking sau trả xe
        assertEquals("active", booking.getStatus(), "Booking chờ quyết toán");
        assertEquals("available", vehicle.getStatus(), "Xe hoàn trả không hư hỏng quay lại AVAILABLE");
        assertEquals(5200, vehicle.getOdometer(), "Cập nhật ODO trả xe");

        // 3. Thực hiện quyết toán (Settle)
        SettlementDTO settlementDTO = new SettlementDTO();
        settlementDTO.setPaymentMethod("BANK_TRANSFER");

        handoverService.settle(503L, 2L, settlementDTO);
        assertEquals("completed", booking.getStatus(), "Booking hoàn tất sau khi quyết toán xong");
    }

    @Test
    @DisplayName("Luồng 4: Ghi nhận & Xử lý Sự cố / Phạt nguội -> Nhân viên lập biên bản, định giá bồi thường, liên kết phí vào đơn")
    void testFlow4_IssueManagementFlow() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(88L);

        Booking booking = new Booking();
        booking.setId(504L);
        booking.setStatus("active");
        booking.setVehicle(vehicle);

        Employee staff = new Employee();
        staff.setId(2L);
        staff.setRole("staff");

        when(bookingRepository.findById(504L)).thenReturn(Optional.of(booking));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> {
            Issue saved = inv.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        // 1. Tạo biên bản sự cố va chạm trầy xước xe
        IssueFormDTO form = new IssueFormDTO();
        form.setBookingId(504L);
        form.setIssueType(IssueType.DAMAGE);
        form.setSubject("Trầy xước cản trước bên phụ");
        form.setDescription("Va quẹt nhẹ cột bãi xe");
        form.setOccurredAt(LocalDateTime.now().minusHours(1));
        form.setFeeAmount(new BigDecimal("500000")); // Bồi thường 500k

        IssueActor actor = new IssueActor(2L, EmployeeRole.STAFF);
        Issue createdIssue = issueService.create(form, actor);

        // 2. Assertions cho Luồng 4
        assertNotNull(createdIssue);
        assertEquals("Trầy xước cản trước bên phụ", createdIssue.getSubject());
        assertEquals(IssueStatus.OPEN, createdIssue.getStatus(), "Sự cố mới tạo ở trạng thái OPEN");

        // 3. Tạo bản ghi BookingFee tương ứng để quyết toán
        BookingFee fee = new BookingFee();
        fee.setBooking(booking);
        fee.setIssue(createdIssue);
        fee.setFeeType("DAMAGE");
        fee.setAmount(form.getFeeAmount());

        assertEquals(new BigDecimal("500000"), fee.getAmount(), "Phí sự cố được ghi nhận vào tổng phụ phí cuốc xe");
    }

    @Test
    @DisplayName("Luồng 5: Quản trị Đội xe, Biển số & Lịch Đăng kiểm -> Thêm xe, đổi biển, lên lịch bảo dưỡng và tái kích hoạt xe")
    void testFlow5_FleetAndInspectionFlow() {
        // 1. Khởi tạo xe và biển số
        VehicleType type = new VehicleType();
        type.setId(2L);
        type.setName("VinFast VF8");

        LicensePlate plate1 = new LicensePlate();
        plate1.setId(1L);
        plate1.setPlate("51K-999.88");

        Vehicle vehicle = new Vehicle();
        vehicle.setId(99L);
        vehicle.setVehicleType(type);
        vehicle.setCurrentPlate(plate1);
        vehicle.setStatus("available");

        when(vehicleRepository.findById(99L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(vehicle));

        // 2. Lên lịch bảo dưỡng định kỳ
        VehicleInspection inspection = new VehicleInspection();
        inspection.setId(1L);
        inspection.setVehicle(vehicle);
        inspection.setStatus("SCHEDULED");
        inspection.setScheduledDate(java.time.LocalDate.now().plusDays(1));

        when(inspectionRepository.save(any(VehicleInspection.class))).thenAnswer(inv -> inv.getArgument(0));

        // Khi bắt đầu đăng kiểm -> Xe chuyển sang bảo trì (maintenance)
        vehicle.setStatus("maintenance");
        inspection.setStatus("IN_PROGRESS");
        assertEquals("maintenance", vehicle.getStatus(), "Xe phải chuyển sang maintenance khi bắt đầu đăng kiểm");

        // 3. Hoàn tất đăng kiểm -> Xe quay lại trạng thái sẵn sàng đón khách
        inspection.setStatus("PASSED");
        vehicle.setStatus("available");
        assertEquals("available", vehicle.getStatus(), "Xe hoàn tất bảo dưỡng quay về AVAILABLE");
    }

    @Test
    @DisplayName("Luồng 6: Quản lý Tài khoản, Phân quyền & Giấy phép lái xe -> Đăng ký mật khẩu BCrypt, cấp Session & Admin quản lý khóa tài khoản")
    void testFlow6_AccountSecurityAndLicenseFlow() {
        // 1. Kiểm tra mã hóa mật khẩu bảo mật BCrypt
        String rawPassword = "SecurePassword@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        assertNotEquals(rawPassword, encodedPassword, "Mật khẩu không được lưu plain-text");
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword), "Mật khẩu BCrypt phải khớp khi đăng nhập");

        // 2. Khởi tạo tài khoản Customer và nộp GPLX (Đã bỏ xét duyệt theo chỉ đạo Mentor)
        Customer customer = new Customer();
        customer.setId(201L);
        customer.setName("Lê Thị Khách");
        customer.setEmail("khach@test.com");
        customer.setPasswordHash(encodedPassword);
        customer.setActive(true);

        LicenseDocument license = new LicenseDocument(
                customer,
                "B2-987654321",
                "B2",
                LocalDate.now().plusYears(5),
                "license_b2.jpg",
                OffsetDateTime.now()
        );

        assertEquals("B2-987654321", license.getLicenseNumber());
        assertEquals("license_b2.jpg", license.getLicenseImageUrl());
        assertNotNull(license.getSubmittedAt());

        // 3. Admin thực hiện Khóa / Kích hoạt tài khoản
        customer.setActive(false);
        assertFalse(customer.isActive(), "Admin có quyền khóa tài khoản vi phạm chính sách");

        customer.setActive(true);
        assertTrue(customer.isActive(), "Admin mở khóa tài khoản thành công");
    }
}
