package com.crms.mvc.service.impl;

import com.crms.mvc.dto.PriceQuoteDTO;
import com.crms.mvc.dto.VehicleScheduleDTO;
import com.crms.mvc.dto.VehicleTypeSearchResultDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleInspection;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.service.InspectionRules;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.service.VehicleAvailabilityService;
import com.crms.mvc.util.DateTimeUtils;
import com.crms.mvc.util.VehicleImages;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class VehicleAvailabilityServiceImpl implements VehicleAvailabilityService {
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ContractTemplateService contractTemplateService;
    private final PricingService pricingService;

    public VehicleAvailabilityServiceImpl(BookingRepository bookingRepository,
                                          VehicleRepository vehicleRepository,
                                          VehicleTypeRepository vehicleTypeRepository,
                                          ContractTemplateService contractTemplateService,
                                          PricingService pricingService) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.contractTemplateService = contractTemplateService;
        this.pricingService = pricingService;
    }

    @Override
    public List<Vehicle> findAvailableVehiclesInTimeRange(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime) {
        List<Vehicle> allVehiclesOfType = vehicleRepository.findByVehicleTypeIdWithDetails(vehicleTypeId);

        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES);
        OffsetDateTime bufferedStart = startTime.minusHours(BUFFER_BETWEEN_RENTALS_HOURS);
        OffsetDateTime bufferedEnd = endTime.plusHours(BUFFER_BETWEEN_RENTALS_HOURS);
        List<Long> bookedVehicleIds = bookingRepository.findBookedVehicleIdsInTimeRange(
                bufferedStart, bufferedEnd, cutoffTime, OffsetDateTime.now());

        ZoneOffset vietnam = DateTimeUtils.VIETNAM_OFFSET;
        LocalDate startDay = startTime.withOffsetSameInstant(vietnam).toLocalDate();
        // The last moment the car is driven is just before the end: a rental ending at 00:00 ends on the day before.
        LocalDate endDay = endTime.minusNanos(1).withOffsetSameInstant(vietnam).toLocalDate();
        Map<Long, List<LocalDate>> inspectionDays = new HashMap<>();
        for (VehicleInspection inspection : vehicleRepository.findScheduledInspectionsByVehicleTypeId(vehicleTypeId)) {
            inspectionDays.computeIfAbsent(inspection.getVehicle().getId(), id -> new ArrayList<>())
                    .add(inspection.getScheduledDate());
        }

        return allVehiclesOfType.stream()
                .filter(v -> !Vehicle.STATUS_MAINTENANCE.equalsIgnoreCase(v.getStatus()) && !Vehicle.STATUS_RETIRED.equalsIgnoreCase(v.getStatus()))
                .filter(v -> !bookedVehicleIds.contains(v.getId()))
                .filter(v -> !InspectionRules.blocksRental(
                        v.getRegistrationExpiry(), inspectionDays.getOrDefault(v.getId(), List.of()), startDay, endDay))
                .toList();
    }

    @Override
    public VehicleScheduleDTO getVehicleSchedule(Long vehicleId, LocalDate fromDate, int days) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy xe: " + vehicleId));

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = fromDate.atStartOfDay().atOffset(DateTimeUtils.VIETNAM_OFFSET);
        OffsetDateTime to = from.plusDays(days);
        List<OffsetDateTime[]> held = bookingRepository.findBookingsHoldingVehicle(vehicleId,
                        from.minusHours(BUFFER_BETWEEN_RENTALS_HOURS), to.plusHours(BUFFER_BETWEEN_RENTALS_HOURS),
                        now.minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES))
                .stream()
                .map(b -> new OffsetDateTime[]{b.getPickupTime(), heldUntil(b, now)})
                .toList();

        // Days the customer cannot book for inspection reasons are marked too, so a free-looking hour is really bookable.
        List<LocalDate> inspectionDays = vehicleRepository.findScheduledInspectionsByVehicleId(vehicleId).stream()
                .map(VehicleInspection::getScheduledDate)
                .toList();

        List<VehicleScheduleDTO.Day> scheduleDays = new ArrayList<>();
        for (int day = 0; day < days; day++) {
            LocalDate date = fromDate.plusDays(day);
            InspectionRules.DayBlock block = InspectionRules.blockOn(
                    vehicle.getRegistrationExpiry(), inspectionDays, date);
            List<VehicleScheduleDTO.HourState> hours = new ArrayList<>(24);
            for (int hour = 0; hour < 24; hour++) {
                OffsetDateTime hourStart = from.plusDays(day).plusHours(hour);
                VehicleScheduleDTO.HourState state = hourState(hourStart, hourStart.plusHours(1), held, now);
                if (state == VehicleScheduleDTO.HourState.FREE) {
                    state = switch (block) {
                        case INSPECTION_DAY -> VehicleScheduleDTO.HourState.INSPECTION;
                        case EXPIRED -> VehicleScheduleDTO.HourState.EXPIRED;
                        case NONE -> state;
                    };
                }
                hours.add(state);
            }
            scheduleDays.add(new VehicleScheduleDTO.Day(date, hours));
        }

        VehicleScheduleDTO schedule = new VehicleScheduleDTO();
        schedule.setVehicleId(vehicle.getId());
        schedule.setLicensePlate(vehicle.getLicensePlate() != null ? vehicle.getLicensePlate().getPlate() : null);
        schedule.setVehicleTypeId(vehicle.getVehicleType().getId());
        schedule.setVehicleTypeName(vehicle.getVehicleType().getDisplayName());
        schedule.setVehicleStatus(vehicle.getStatus());
        schedule.setDays(scheduleDays);
        return schedule;
    }

    /** A car out on a rental that has not come back stays held until it does, even past its scheduled end. */
    private static OffsetDateTime heldUntil(Booking booking, OffsetDateTime now) {
        boolean notBackYet = Booking.STATUS_ACTIVE.equalsIgnoreCase(booking.getStatus()) && booking.getActualReturnTime() == null;
        return notBackYet && now.isAfter(booking.getScheduledEndTime()) ? now : booking.getScheduledEndTime();
    }

    private static VehicleScheduleDTO.HourState hourState(OffsetDateTime start, OffsetDateTime end,
                                                          List<OffsetDateTime[]> held, OffsetDateTime now) {
        if (!end.isAfter(now)) {
            return VehicleScheduleDTO.HourState.PAST;
        }
        if (held.stream().anyMatch(h -> overlaps(start, end, h[0], h[1]))) {
            return VehicleScheduleDTO.HourState.BOOKED;
        }
        // The hour we are in has already begun, and a rental starts on a whole hour: the next hour is the earliest.
        if (start.isBefore(now)) {
            return VehicleScheduleDTO.HourState.PAST;
        }
        boolean inBuffer = held.stream().anyMatch(h ->
                overlaps(start, end, h[0].minusHours(BUFFER_BETWEEN_RENTALS_HOURS), h[0])
                        || overlaps(start, end, h[1], h[1].plusHours(BUFFER_BETWEEN_RENTALS_HOURS)));
        return inBuffer ? VehicleScheduleDTO.HourState.BUFFER : VehicleScheduleDTO.HourState.FREE;
    }

    private static boolean overlaps(OffsetDateTime start, OffsetDateTime end, OffsetDateTime otherStart, OffsetDateTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }

    @Override
    public List<VehicleTypeSearchResultDTO> searchAvailableVehicleTypes(OffsetDateTime startTime, OffsetDateTime endTime, String category) {
        List<VehicleType> allTypes = vehicleTypeRepository.findAll();
        List<VehicleTypeSearchResultDTO> results = new ArrayList<>();

        BigDecimal depositRate = contractTemplateService.getActiveTemplate().getDepositRate();

        for (VehicleType vt : allTypes) {
            if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) {
                if (!category.equalsIgnoreCase(vt.getCategory())) {
                    continue;
                }
            }

            List<Vehicle> availableCars = findAvailableVehiclesInTimeRange(vt.getId(), startTime, endTime);

            VehicleTypeSearchResultDTO dto = new VehicleTypeSearchResultDTO();
            dto.setId(vt.getId());
            dto.setName(vt.getName());
            dto.setBrand(vt.getBrand());
            dto.setCategory(vt.getCategory());
            dto.setSeats(vt.getSeats());
            dto.setTransmission(vt.getTransmission());
            dto.setFuelType(vt.getFuelType());
            dto.setPricePerHour(vt.getPricePerHour());

            PriceQuoteDTO quote = pricingService.calculatePrice(startTime, endTime, vt.getPricePerHour(), depositRate, BigDecimal.ZERO);
            dto.setEstimatedTotal(quote.getTotalPrice());
            dto.setDepositAmount(quote.getDepositAmount());
            dto.setImageUrl(VehicleImages.urlOf(vt));
            dto.setAvailableCount(availableCars.size());

            results.add(dto);
        }

        return results;
    }
}
