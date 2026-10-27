package com.crms.mvc.dto;

import com.crms.mvc.entity.LicensePlate;

import java.io.Serializable;
import java.time.OffsetDateTime;

/** One plate in a vehicle's history. */
public class LicensePlateDTO implements Serializable {
    private Long id;
    private String plate;
    private String province;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;

    public static LicensePlateDTO fromEntity(LicensePlate licensePlate) {
        LicensePlateDTO dto = new LicensePlateDTO();
        dto.setId(licensePlate.getId());
        dto.setPlate(licensePlate.getPlate());
        dto.setProvince(licensePlate.getProvince());
        dto.setValidFrom(licensePlate.getValidFrom());
        dto.setValidTo(licensePlate.getValidTo());
        return dto;
    }

    public boolean isCurrent() {
        return validTo == null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public OffsetDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(OffsetDateTime validFrom) { this.validFrom = validFrom; }

    public OffsetDateTime getValidTo() { return validTo; }
    public void setValidTo(OffsetDateTime validTo) { this.validTo = validTo; }
}
