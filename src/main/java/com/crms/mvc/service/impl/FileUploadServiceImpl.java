package com.crms.mvc.service.impl;

import com.crms.mvc.service.FileUploadService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Path LICENSE_UPLOAD_DIRECTORY =
            Path.of("uploads", "licenses").toAbsolutePath().normalize();
    private static final Path VEHICLE_UPLOAD_DIRECTORY =
            Path.of("uploads", "vehicles").toAbsolutePath().normalize();
    // Kept outside every public resource mapping; served only through ContractController.
    private static final Path CONTRACT_DIRECTORY =
            Path.of("uploads", "contracts").toAbsolutePath().normalize();

    @Override
    public String saveLicenseImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("License image is required");
        }

        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image must be 5 MB or smaller");
        }

        String extension = switch (image.getContentType() == null ? "" : image.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        };

        String fileName = UUID.randomUUID() + extension;
        Path destination = LICENSE_UPLOAD_DIRECTORY.resolve(fileName);

        try {
            Files.createDirectories(LICENSE_UPLOAD_DIRECTORY);
            Files.copy(image.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save license image", exception);
        }
    }

    @Override
    public Resource loadLicenseImage(String fileName) {
        return loadFrom(LICENSE_UPLOAD_DIRECTORY, fileName);
    }

    @Override
    public String saveVehicleImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Vehicle image is required");
        }

        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image must be 5 MB or smaller");
        }

        String extension = switch (image.getContentType() == null ? "" : image.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        };

        String fileName = "vehicle-" + UUID.randomUUID() + extension;
        Path destination = VEHICLE_UPLOAD_DIRECTORY.resolve(fileName);

        try {
            Files.createDirectories(VEHICLE_UPLOAD_DIRECTORY);
            Files.copy(image.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/vehicles/" + fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save vehicle image", exception);
        }
    }

    @Override
    public Resource loadVehicleImage(String fileName) {
        return loadFrom(VEHICLE_UPLOAD_DIRECTORY, fileName);
    }

    @Override
    public String saveContractPdf(String contractNumber, byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Contract PDF is empty");
        }

        String fileName = contractNumber + "-" + UUID.randomUUID() + ".pdf";
        try {
            Files.createDirectories(CONTRACT_DIRECTORY);
            // CREATE_NEW: an issued contract file must never be overwritten.
            Files.write(CONTRACT_DIRECTORY.resolve(fileName), content, StandardOpenOption.CREATE_NEW);
            return fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save contract PDF", exception);
        }
    }

    @Override
    public Resource loadContractPdf(String fileName) {
        return loadFrom(CONTRACT_DIRECTORY, fileName);
    }

    private Resource loadFrom(Path directory, String fileName) {
        if (fileName == null || !fileName.equals(Path.of(fileName).getFileName().toString())) {
            return null;
        }

        Path filePath = directory.resolve(fileName).normalize();
        if (!filePath.startsWith(directory)
                || !Files.isRegularFile(filePath)
                || !Files.isReadable(filePath)) {
            return null;
        }

        return new FileSystemResource(filePath);
    }
}
