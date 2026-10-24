package com.crms.mvc.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String saveLicenseImage(MultipartFile image);

    Resource loadLicenseImage(String fileName);

    String saveVehicleImage(MultipartFile image);

    Resource loadVehicleImage(String fileName);

    /** Stores a generated contract PDF in a private folder and returns its file name. */
    String saveContractPdf(String contractNumber, byte[] content);

    Resource loadContractPdf(String fileName);
}
