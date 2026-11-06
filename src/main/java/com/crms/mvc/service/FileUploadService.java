package com.crms.mvc.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String saveLicenseImage(MultipartFile image);

    Resource loadLicenseImage(String fileName);

    String saveVehicleImage(MultipartFile image);

    Resource loadVehicleImage(String fileName);

    String saveHandoverImage(MultipartFile image);

    Resource loadHandoverImage(String fileName);

    /** Stores a generated contract PDF in a private folder and returns its file name. */
    String saveContractPdf(String contractNumber, byte[] content);

    Resource loadContractPdf(String fileName);

    /** Stores one incident photo in a private folder (never publicly mapped) and returns its file name. */
    String saveIssueEvidence(MultipartFile image);

    Resource loadIssueEvidence(String fileName);

    void deleteIssueEvidence(String fileName);

    MediaType resolveMediaType(String fileName);
}
