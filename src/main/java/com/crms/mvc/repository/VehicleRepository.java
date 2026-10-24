package com.crms.mvc.repository;

import com.crms.mvc.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    /** Keyword (already a lower-case LIKE pattern) matches model, brand, current plate or colour. */
    String ADMIN_SEARCH_FILTER = "WHERE (:status = 'all' OR v.status = :status) AND ("
            + "LOWER(vt.brand) LIKE :keyword ESCAPE '\\' OR LOWER(vt.name) LIKE :keyword ESCAPE '\\' "
            + "OR LOWER(lp.plate) LIKE :keyword ESCAPE '\\' OR LOWER(v.color) LIKE :keyword ESCAPE '\\')";

    @Query(value = "SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN v.currentPlates lp " + ADMIN_SEARCH_FILTER,
            countQuery = "SELECT COUNT(v) FROM Vehicle v JOIN v.vehicleType vt LEFT JOIN v.currentPlates lp " + ADMIN_SEARCH_FILTER)
    Page<Vehicle> searchForAdmin(@Param("status") String status,
                                 @Param("keyword") String keywordPattern,
                                 Pageable pageable);

    interface StatusCount {
        String getStatus();

        long getTotal();
    }

    @Query("SELECT v.status AS status, COUNT(v) AS total FROM Vehicle v GROUP BY v.status")
    List<StatusCount> countGroupedByStatus();

    List<Vehicle> findAllByOrderByIdAsc();

    List<Vehicle> findByVehicleTypeBrandContainingIgnoreCaseOrVehicleTypeNameContainingIgnoreCaseOrderByIdAsc(String brand, String name);

    long countByVehicleTypeId(Long vehicleTypeId);

    List<Vehicle> findByStatusOrderByIdAsc(String status);

    List<Vehicle> findByStatus(String status);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.currentPlates lp WHERE v.status = :status")
    List<Vehicle> findByStatusWithDetails(@Param("status") String status);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.currentPlates lp WHERE v.id = :id")
    Optional<Vehicle> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.currentPlates lp ORDER BY v.id ASC")
    List<Vehicle> findAllWithDetails();

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.currentPlates lp WHERE vt.id = :typeId ORDER BY v.id ASC")
    List<Vehicle> findByVehicleTypeIdWithDetails(@Param("typeId") Long typeId);

}
