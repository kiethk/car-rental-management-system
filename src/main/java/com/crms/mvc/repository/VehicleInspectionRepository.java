package com.crms.mvc.repository;
import com.crms.mvc.entity.VehicleInspection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleInspectionRepository extends JpaRepository<VehicleInspection, Long> {
}
