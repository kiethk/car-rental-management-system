package com.crms.mvc.repository;
import com.crms.mvc.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByVehicleTypeBrandContainingIgnoreCaseOrVehicleTypeNameContainingIgnoreCaseOrderByIdAsc(String brand, String name);
}
