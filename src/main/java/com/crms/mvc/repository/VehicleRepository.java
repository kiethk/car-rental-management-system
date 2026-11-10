package com.crms.mvc.repository;

import com.crms.mvc.enums.VehicleStatus;
import com.crms.mvc.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import com.crms.mvc.entity.VehicleInspection;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    /**
     * Locks every vehicle of a type (SELECT ... FOR UPDATE) so two bookings for that type are placed one after the
     * other: the second waits, then sees the first one's vehicle as taken.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.vehicleType.id = :typeId")
    List<Vehicle> lockByVehicleTypeId(@Param("typeId") Long typeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);

    /** Keyword (already a lower-case LIKE pattern) matches model, brand, "brand model" together, current plate or colour. */
    String ADMIN_SEARCH_FILTER = "WHERE (:allStatuses = true OR v.status = :status) AND ("
            + "LOWER(vt.brand) LIKE :keyword ESCAPE '\\' OR LOWER(vt.name) LIKE :keyword ESCAPE '\\' "
            + "OR LOWER(CONCAT(vt.brand, ' ', vt.name)) LIKE :keyword ESCAPE '\\' "
            + "OR LOWER(lp.plate) LIKE :keyword ESCAPE '\\' OR LOWER(v.color) LIKE :keyword ESCAPE '\\')";

    @Query(value = "SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN v.currentPlates lp " + ADMIN_SEARCH_FILTER,
            countQuery = "SELECT COUNT(v) FROM Vehicle v JOIN v.vehicleType vt LEFT JOIN v.currentPlates lp " + ADMIN_SEARCH_FILTER)
    Page<Vehicle> searchForAdmin(@Param("allStatuses") boolean allStatuses,
                                 @Param("status") VehicleStatus status,
                                 @Param("keyword") String keywordPattern,
                                 Pageable pageable);

    interface StatusCount {
        VehicleStatus getStatus();

        long getTotal();
    }

    @Query("SELECT v.status AS status, COUNT(v) AS total FROM Vehicle v GROUP BY v.status")
    List<StatusCount> countGroupedByStatus();

    List<Vehicle> findAllByOrderByIdAsc();

    List<Vehicle> findByVehicleTypeBrandContainingIgnoreCaseOrVehicleTypeNameContainingIgnoreCaseOrderByIdAsc(String brand, String name);

    long countByVehicleTypeId(Long vehicleTypeId);

    List<Vehicle> findByStatusOrderByIdAsc(VehicleStatus status);

    List<Vehicle> findByStatus(VehicleStatus status);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.currentPlates lp WHERE v.status = :status")
    List<Vehicle> findByStatusWithDetails(@Param("status") VehicleStatus status);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.currentPlates lp WHERE v.id = :id")
    Optional<Vehicle> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.currentPlates lp ORDER BY v.id ASC")
    List<Vehicle> findAllWithDetails();

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.currentPlates lp WHERE vt.id = :typeId ORDER BY v.id ASC")
    List<Vehicle> findByVehicleTypeIdWithDetails(@Param("typeId") Long typeId);

    /** Inspections of this car that are scheduled and not done yet. */
    @Query("SELECT i FROM VehicleInspection i WHERE i.vehicle.id = :vehicleId AND i.status = com.crms.mvc.enums.InspectionStatus.SCHEDULED")
    List<VehicleInspection> findScheduledInspectionsByVehicleId(@Param("vehicleId") Long vehicleId);

    /** Inspections of this vehicle type's cars that are scheduled and not done yet. */
    @Query("SELECT i FROM VehicleInspection i JOIN FETCH i.vehicle v WHERE v.vehicleType.id = :typeId AND i.status = com.crms.mvc.enums.InspectionStatus.SCHEDULED")
    List<VehicleInspection> findScheduledInspectionsByVehicleTypeId(@Param("typeId") Long typeId);

}
