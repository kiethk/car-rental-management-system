package com.crms.mvc.service.impl;

import com.crms.mvc.dto.PriceQuoteDTO;
import com.crms.mvc.dto.VehicleTypeSearchResultDTO;
import com.crms.mvc.entity.Slot;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.SlotRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.service.SlotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final ContractTemplateService contractTemplateService;
    private final PricingService pricingService;

    public SlotServiceImpl(
            SlotRepository slotRepository,
            VehicleRepository vehicleRepository,
            VehicleTypeRepository vehicleTypeRepository,
            BookingRepository bookingRepository,
            ContractTemplateService contractTemplateService,
            PricingService pricingService) {
        this.slotRepository = slotRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.bookingRepository = bookingRepository;
        this.contractTemplateService = contractTemplateService;
        this.pricingService = pricingService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> findAvailableVehiclesInTimeRange(Long vehicleTypeId, OffsetDateTime startTime, OffsetDateTime endTime) {
        // B2: Xe trống cho khung giờ tương lai được lấy từ tất cả xe thuộc loại này,
        // chỉ loại trừ xe đang bảo trì (maintenance) hoặc đang có booking trùng giờ.
        List<Vehicle> allVehiclesOfType = vehicleRepository.findByVehicleTypeIdWithDetails(vehicleTypeId);

        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(15);
        List<Long> bookedVehicleIds = bookingRepository.findBookedVehicleIdsInTimeRange(startTime, endTime, cutoffTime);

        return allVehiclesOfType.stream()
                .filter(v -> !"maintenance".equalsIgnoreCase(v.getStatus()))
                .filter(v -> !bookedVehicleIds.contains(v.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
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

            // B5: Thống nhất tính giá qua PricingService
            PriceQuoteDTO quote = pricingService.calculatePrice(startTime, endTime, vt.getPricePerHour(), depositRate, BigDecimal.ZERO);
            dto.setEstimatedTotal(quote.getTotalPrice());
            dto.setDepositAmount(quote.getDepositAmount());

            String img = vt.getImageUrl();
            if (img == null || img.isBlank()) {
                img = "https://images.unsplash.com/photo-1619682817481-e994891cd1f5?auto=format&fit=crop&w=800&q=80";
            }
            dto.setImageUrl(img);
            dto.setRating(4.8);
            dto.setReviewCount(120);
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
