package com.crms.mvc.repository;

import com.crms.mvc.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    List<VehicleType> findAllByOrderByIdAsc();

    @Query("SELECT DISTINCT vt.brand FROM VehicleType vt ORDER BY vt.brand ASC")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT vt.category FROM VehicleType vt ORDER BY vt.category ASC")
    List<String> findDistinctCategories();

    boolean existsByNameIgnoreCaseAndBrandIgnoreCase(String name, String brand);

    boolean existsByNameIgnoreCaseAndBrandIgnoreCaseAndIdNot(String name, String brand, Long id);
}
