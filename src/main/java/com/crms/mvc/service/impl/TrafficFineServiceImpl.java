package com.crms.mvc.service.impl;

import com.crms.mvc.dto.TrafficFineFormDTO;
import com.crms.mvc.dto.TrafficFineNoticeDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.TrafficFineNotice;
import com.crms.mvc.event.TrafficFineRecordedEvent;
import com.crms.mvc.event.TrafficFineRemovedEvent;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.repository.TrafficFineNoticeRepository;
import com.crms.mvc.service.ContractService;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.TrafficFineService;
import com.crms.mvc.util.DateTimeUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class TrafficFineServiceImpl implements TrafficFineService {

    private final TrafficFineNoticeRepository noticeRepository;
    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final LicensePlateRepository licensePlateRepository;
    private final ContractService contractService;
    private final FileUploadService fileUploadService;
    private final ApplicationEventPublisher eventPublisher;

    public TrafficFineServiceImpl(TrafficFineNoticeRepository noticeRepository,
                                  BookingRepository bookingRepository,
                                  EmployeeRepository employeeRepository,
                                  LicensePlateRepository licensePlateRepository,
                                  ContractService contractService,
                                  FileUploadService fileUploadService,
                                  ApplicationEventPublisher eventPublisher) {
        this.noticeRepository = noticeRepository;
        this.bookingRepository = bookingRepository;
        this.employeeRepository = employeeRepository;
        this.licensePlateRepository = licensePlateRepository;
        this.contractService = contractService;
        this.fileUploadService = fileUploadService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public Booking findBookingForViolation(String plate, LocalDateTime violationAt) {
        OffsetDateTime at = violationAt.atZone(DateTimeUtils.VIETNAM_ZONE).toOffsetDateTime();
        LicensePlate onTheCar = licensePlateRepository.findByPlateIgnoreCase(plate == null ? "" : plate.trim())
                .filter(p -> !p.getValidFrom().isAfter(at) && (p.getValidTo() == null || p.getValidTo().isAfter(at)))
                .orElseThrow(() -> new NoSuchElementException("Không có xe nào mang biển số này tại thời điểm vi phạm"));

        return bookingRepository.findRentalsCovering(onTheCar.getVehicle().getId(), at).stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Không có đơn thuê nào của xe này tại thời điểm vi phạm"));
    }

    @Override
    public TrafficFineNoticeDTO record(TrafficFineFormDTO form, Long employeeId) {
        Booking booking = bookingRepository.findById(form.getBookingId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + form.getBookingId()));
        if (booking.getActualPickupTime() == null) {
            throw new IllegalStateException("Chỉ ghi nhận phạt nguội cho đơn thuê khách đã nhận xe");
        }
        if (form.getImage() == null || form.getImage().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng tải lên ảnh biên bản của công an");
        }
        Employee recorder = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên: " + employeeId));

        TrafficFineNotice notice = new TrafficFineNotice();
        notice.setBooking(booking);
        notice.setPlate(form.getPlate().trim().toUpperCase());
        notice.setViolationAt(form.getViolationAt().atZone(DateTimeUtils.VIETNAM_ZONE).toOffsetDateTime());
        notice.setImageFileName(fileUploadService.saveTrafficFineImage(form.getImage()));
        notice.setRecordedBy(recorder);
        notice.setRecordedAt(OffsetDateTime.now());
        TrafficFineNotice saved = noticeRepository.save(notice);

        TrafficFineNoticeDTO dto = toDto(saved);
        eventPublisher.publishEvent(new TrafficFineRecordedEvent(dto, saved.getImageFileName()));
        return dto;
    }

    @Override
    public void delete(Long noticeId) {
        TrafficFineNotice notice = noticeRepository.findWithDetailsById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bản ghi phạt nguội: " + noticeId));
        TrafficFineNoticeDTO dto = toDto(notice);
        noticeRepository.delete(notice);
        fileUploadService.deleteTrafficFineImage(notice.getImageFileName());
        eventPublisher.publishEvent(new TrafficFineRemovedEvent(dto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrafficFineNoticeDTO> findAll() {
        return noticeRepository.findAllByOrderByRecordedAtDesc().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrafficFineNoticeDTO> findByBookingForCustomer(Long bookingId, Long customerId) {
        return noticeRepository.findByBookingIdOrderByViolationAtDesc(bookingId).stream()
                .filter(n -> customerId != null && customerId.equals(n.getBooking().getCustomer().getId()))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadImage(Long noticeId) {
        TrafficFineNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bản ghi phạt nguội: " + noticeId));
        return fileUploadService.loadTrafficFineImage(notice.getImageFileName());
    }

    private TrafficFineNoticeDTO toDto(TrafficFineNotice notice) {
        Booking booking = notice.getBooking();
        return new TrafficFineNoticeDTO(
                notice.getId(),
                booking.getId(),
                booking.getCustomer().getName(),
                booking.getCustomer().getEmail(),
                notice.getPlate(),
                DateTimeUtils.toVietnamTime(notice.getViolationAt()),
                DateTimeUtils.toVietnamTime(notice.getRecordedAt()),
                notice.getRecordedBy().getName(),
                DateTimeUtils.toVietnamTime(booking.getActualPickupTime()),
                DateTimeUtils.toVietnamTime(booking.getActualReturnTime()),
                contractService.findByBookingId(booking.getId()).map(c -> c.getContractNumber()).orElse(null));
    }
}
