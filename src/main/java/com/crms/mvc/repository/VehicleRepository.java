package com.crms.mvc.repository;

import com.crms.mvc.entity.Vehicle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    @EntityGraph(attributePaths = {"licensePlate", "vehicleType"})
    List<Vehicle> findByVehicleTypeBrandContainingIgnoreCaseOrVehicleTypeNameContainingIgnoreCaseOrderByIdAsc(String brand, String name);

    @EntityGraph(attributePaths = {"licensePlate", "vehicleType"})
    List<Vehicle> findByStatus(String status);
}
