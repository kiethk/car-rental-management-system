package com.crms.mvc.repository;

import com.crms.mvc.entity.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    List<VehicleType> findAllByOrderByIdAsc();

    /** Keyword (already a lower-case LIKE pattern) matches model, brand or category. */
    @Query("SELECT vt FROM VehicleType vt WHERE LOWER(vt.name) LIKE :keyword ESCAPE '\\' "
            + "OR LOWER(vt.brand) LIKE :keyword ESCAPE '\\' OR LOWER(vt.category) LIKE :keyword ESCAPE '\\'")
    Page<VehicleType> searchForAdmin(@Param("keyword") String keywordPattern, Pageable pageable);

    @Query("SELECT DISTINCT vt.brand FROM VehicleType vt ORDER BY vt.brand ASC")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT vt.category FROM VehicleType vt ORDER BY vt.category ASC")
    List<String> findDistinctCategories();

    boolean existsByNameIgnoreCaseAndBrandIgnoreCase(String name, String brand);

    boolean existsByNameIgnoreCaseAndBrandIgnoreCaseAndIdNot(String name, String brand, Long id);
}
