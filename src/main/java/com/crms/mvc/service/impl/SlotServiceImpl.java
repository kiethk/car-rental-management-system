package com.crms.mvc.service.impl;

import com.crms.mvc.dto.VehicleTypeSearchResultDTO;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.SlotRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.SlotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class SlotServiceImpl implements SlotService {
    private final SlotRepository slotRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final BookingRepository bookingRepository;

    public SlotServiceImpl(
            SlotRepository slotRepository,
            VehicleRepository vehicleRepository,
            VehicleTypeRepository vehicleTypeRepository,
            BookingRepository bookingRepository) {
        this.slotRepository = slotRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> findAvailableVehiclesInTimeRange(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime) {
        List<Vehicle> allVehiclesOfType = vehicleRepository.findByStatus("available").stream()
                .filter(v -> v.getVehicleType().getId().equals(vehicleTypeId))
                .toList();

        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(15);
        List<Long> bookedVehicleIds = bookingRepository.findBookedVehicleIdsInTimeRange(startTime, endTime, cutoffTime);

        return allVehiclesOfType.stream()
                .filter(v -> !bookedVehicleIds.contains(v.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeSearchResultDTO> searchAvailableVehicleTypes(OffsetDateTime startTime, OffsetDateTime endTime, String category) {
        List<VehicleType> allTypes = vehicleTypeRepository.findAll();
        List<VehicleTypeSearchResultDTO> results = new ArrayList<>();

        long hours = Math.max(1, Duration.between(startTime, endTime).toHours());

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
            BigDecimal estimatedTotal = vt.getPricePerHour().multiply(BigDecimal.valueOf(hours));
            dto.setEstimatedTotal(estimatedTotal);
            dto.setDepositAmount(estimatedTotal.multiply(BigDecimal.valueOf(0.5)).setScale(0, RoundingMode.HALF_UP));

            String img = vt.getImageUrl();
            if (img == null || img.isBlank() || img.startsWith("/images/")) {
                if (vt.getName() != null && vt.getName().toLowerCase().contains("cr-v")) {
                    img = "https://images.unsplash.com/photo-1606664515524-ed2f786a0bd6?w=800&h=500&fit=crop&auto=format";
                    dto.setRating(4.8);
                    dto.setReviewCount(89);
                } else if (vt.getName() != null && vt.getName().toLowerCase().contains("vf8")) {
                    img = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800&h=500&fit=crop&auto=format";
                    dto.setRating(4.6);
                    dto.setReviewCount(64);
                } else {
                    img = "https://images.unsplash.com/photo-1549317661-bd32c8ce0db2?w=800&h=500&fit=crop&auto=format";
                    dto.setRating(4.7);
                    dto.setReviewCount(142);
                }
            }
            dto.setImageUrl(img);
            dto.setAvailableCount(availableCars.size());

            results.add(dto);
        }

        return results;
    }

    @Override
    public Slot createSlotForBooking(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime) {
        VehicleType type = vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> new NoSuchElementException("VehicleType not found: " + vehicleTypeId));

        Slot slot = new Slot();
        slot.setVehicleType(type);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setCapacity(1);
        slot.setStatus("open");

        return slotRepository.save(slot);
    }
}
