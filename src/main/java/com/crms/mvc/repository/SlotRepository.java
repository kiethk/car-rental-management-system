package com.crms.mvc.repository;

import com.crms.mvc.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByVehicleTypeIdAndStatus(Long vehicleTypeId, String status);

    @Query("SELECT s FROM Slot s WHERE s.vehicleType.id = :vehicleTypeId AND s.status = 'open' AND s.startTime >= :now ORDER BY s.startTime ASC")
    List<Slot> findAvailableSlotsForType(@Param("vehicleTypeId") Long vehicleTypeId, @Param("now") OffsetDateTime now);
}
