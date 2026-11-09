package com.crms.mvc.service.impl;

import com.crms.mvc.service.FileUploadService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Path LICENSE_UPLOAD_DIRECTORY =
            Path.of("uploads", "licenses").toAbsolutePath().normalize();
    private static final Path HANDOVER_UPLOAD_DIRECTORY =
            Path.of("uploads", "handovers").toAbsolutePath().normalize();
    private static final Path VEHICLE_UPLOAD_DIRECTORY =
            Path.of("uploads", "vehicles").toAbsolutePath().normalize();
    private static final Path PROMOTION_UPLOAD_DIRECTORY =
            Path.of("uploads", "promotions").toAbsolutePath().normalize();
    // Kept outside every public resource mapping; served only through ContractController.
    private static final Path CONTRACT_DIRECTORY =
            Path.of("uploads", "contracts").toAbsolutePath().normalize();
    // Incident photos are evidence about customers: private, served only by controllers that check access.
    private static final Path ISSUE_EVIDENCE_DIRECTORY =
            Path.of("uploads", "issues").toAbsolutePath().normalize();

    @Override
    public String saveLicenseImage(MultipartFile image) {
        String extension = validatedImageExtension(image, "Ảnh giấy phép lái xe");
        String fileName = UUID.randomUUID() + extension;
        Path destination = LICENSE_UPLOAD_DIRECTORY.resolve(fileName);

        try {
            Files.createDirectories(LICENSE_UPLOAD_DIRECTORY);
            Files.copy(image.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh giấy phép lái xe", exception);
        }
    }

    @Override
    public Resource loadLicenseImage(String fileName) {
        return loadFrom(LICENSE_UPLOAD_DIRECTORY, fileName);
    }

    @Override
    public String saveVehicleImage(MultipartFile image) {
        String extension = validatedImageExtension(image, "Ảnh phương tiện");
        String fileName = "vehicle-" + UUID.randomUUID() + extension;
        Path destination = VEHICLE_UPLOAD_DIRECTORY.resolve(fileName);

        try {
            Files.createDirectories(VEHICLE_UPLOAD_DIRECTORY);
            Files.copy(image.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/vehicles/" + fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh phương tiện", exception);
        }
    }

    @Override
    public Resource loadVehicleImage(String fileName) {
        return loadFrom(VEHICLE_UPLOAD_DIRECTORY, fileName);
    }

    @Override
    public String savePromotionImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh khuyến mãi");
        }

        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Ảnh tối đa 5 MB");
        }

        String extension = switch (image.getContentType() == null ? "" : image.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPEG, PNG và WebP");
        };

        String fileName = "promotion-" + UUID.randomUUID() + extension;
        try {
            Files.createDirectories(PROMOTION_UPLOAD_DIRECTORY);
            Files.copy(image.getInputStream(), PROMOTION_UPLOAD_DIRECTORY.resolve(fileName));
            return "/uploads/promotions/" + fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh khuyến mãi", exception);
        }
    }

    @Override
    public String saveHandoverImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh bàn giao");
        }

        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Ảnh tối đa 5 MB");
        }

        String extension = switch (image.getContentType() == null ? "" : image.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPEG, PNG và WebP");
        };

        String fileName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(HANDOVER_UPLOAD_DIRECTORY);
            Files.copy(image.getInputStream(), HANDOVER_UPLOAD_DIRECTORY.resolve(fileName));
            return fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh bàn giao", exception);
        }
    }

    @Override
    public Resource loadHandoverImage(String fileName) {
        return loadFrom(HANDOVER_UPLOAD_DIRECTORY, fileName);
    }

    @Override
    public String saveContractPdf(String contractNumber, byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Nội dung hợp đồng PDF không được để trống");
        }

        String fileName = contractNumber + "-" + UUID.randomUUID() + ".pdf";
        try {
            Files.createDirectories(CONTRACT_DIRECTORY);
            // CREATE_NEW: an issued contract file must never be overwritten.
            Files.write(CONTRACT_DIRECTORY.resolve(fileName), content, StandardOpenOption.CREATE_NEW);
            return fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu tệp hợp đồng PDF", exception);
        }
    }

    @Override
    public Resource loadContractPdf(String fileName) {
        return loadFrom(CONTRACT_DIRECTORY, fileName);
    }

    @Override
    public String saveIssueEvidence(MultipartFile image) {
        String extension = validatedImageExtension(image, "Ảnh bằng chứng sự cố");
        String fileName = "issue-" + UUID.randomUUID() + extension;
        try {
            Files.createDirectories(ISSUE_EVIDENCE_DIRECTORY);
            Files.copy(image.getInputStream(), ISSUE_EVIDENCE_DIRECTORY.resolve(fileName));
            return fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh bằng chứng sự cố", exception);
        }
    }

    @Override
    public Resource loadIssueEvidence(String fileName) {
        return loadFrom(ISSUE_EVIDENCE_DIRECTORY, fileName);
    }

    @Override
    public void deleteIssueEvidence(String fileName) {
        Resource existing = loadFrom(ISSUE_EVIDENCE_DIRECTORY, fileName);
        if (existing == null) {
            return;
        }
        try {
            Files.deleteIfExists(ISSUE_EVIDENCE_DIRECTORY.resolve(fileName).normalize());
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xoá ảnh bằng chứng sự cố", exception);
        }
    }

    @Override
    public MediaType resolveMediaType(String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (name.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    /** Chấp nhận định dạng ảnh JPEG, PNG hoặc WebP tối đa 5 MB và trả về phần mở rộng tệp tương ứng. */
    private String validatedImageExtension(MultipartFile image, String label) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(label + " không được để trống");
        }
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng ảnh phải từ 5 MB trở xuống");
        }
        String contentType = image.getContentType() == null ? "" : image.getContentType().toLowerCase(Locale.ROOT);
        String originalFilename = image.getOriginalFilename() == null ? "" : image.getOriginalFilename().toLowerCase(Locale.ROOT);

        if ("image/jpeg".equals(contentType) || originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg")) {
            return ".jpg";
        }
        if ("image/png".equals(contentType) || originalFilename.endsWith(".png")) {
            return ".png";
        }
        if ("image/webp".equals(contentType) || originalFilename.endsWith(".webp")) {
            return ".webp";
        }
        throw new IllegalArgumentException("Chỉ chấp nhận định dạng ảnh JPEG, PNG hoặc WebP");
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
