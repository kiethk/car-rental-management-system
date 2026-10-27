package com.crms.mvc.service;

import com.crms.mvc.dto.LicensePlateChangeDTO;
import com.crms.mvc.dto.LicensePlateDTO;
import com.crms.mvc.dto.LicensePlateOverviewDTO;

import java.util.List;

public interface LicensePlateService {
    /** One row per vehicle: the plate currently on it and its registration expiry. */
    List<LicensePlateOverviewDTO> findAllOverviews();

    /** Every plate the vehicle has had, newest first; the current one has no end date. */
    List<LicensePlateDTO> findHistory(Long vehicleId);

    /**
     * Puts a new plate on the vehicle. The current plate (if any) is retired now and the new one
     * is valid from now; retired plates are kept as history and can never be reused.
     */
    LicensePlateDTO changePlate(Long vehicleId, LicensePlateChangeDTO form);

    boolean isPlateExists(String plate);
}
