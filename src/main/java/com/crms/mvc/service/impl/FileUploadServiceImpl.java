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
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Path LICENSE_UPLOAD_DIRECTORY =
            Path.of("uploads", "licenses").toAbsolutePath().normalize();
    private static final Path VEHICLE_UPLOAD_DIRECTORY =
            Path.of("uploads", "vehicles").toAbsolutePath().normalize();

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
        if (fileName == null || !fileName.equals(Path.of(fileName).getFileName().toString())) {
            return null;
        }

        Path imagePath = LICENSE_UPLOAD_DIRECTORY.resolve(fileName).normalize();
        if (!imagePath.startsWith(LICENSE_UPLOAD_DIRECTORY)
                || !Files.isRegularFile(imagePath)
                || !Files.isReadable(imagePath)) {
            return null;
        }

        return new FileSystemResource(imagePath);
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
        if (fileName == null || !fileName.equals(Path.of(fileName).getFileName().toString())) {
            return null;
        }

        Path imagePath = VEHICLE_UPLOAD_DIRECTORY.resolve(fileName).normalize();
        if (!imagePath.startsWith(VEHICLE_UPLOAD_DIRECTORY)
                || !Files.isRegularFile(imagePath)
                || !Files.isReadable(imagePath)) {
            return null;
        }

        return new FileSystemResource(imagePath);
    }
}
