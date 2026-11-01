package com.crms.mvc.service;

import com.crms.mvc.service.impl.FileUploadServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Incident photos are private evidence: only real, small pictures are stored, and only by their generated name. */
class IssueEvidenceStorageTest {
    private final FileUploadService storage = new FileUploadServiceImpl();

    @Test
    void aPhotoCanBeStoredReadBackAndDeleted() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("image", "scratch.png", "image/png", new byte[]{1, 2, 3, 4});

        String fileName = storage.saveIssueEvidence(photo);
        try {
            assertTrue(fileName.endsWith(".png"));
            Resource saved = storage.loadIssueEvidence(fileName);
            assertNotNull(saved);
            assertEquals(4, saved.contentLength());
        } finally {
            storage.deleteIssueEvidence(fileName);
        }
        assertNull(storage.loadIssueEvidence(fileName));
    }

    @Test
    void onlyJpegPngAndWebpAreAccepted() {
        MockMultipartFile pdf = new MockMultipartFile("image", "a.pdf", "application/pdf", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> storage.saveIssueEvidence(pdf));
    }

    @Test
    void emptyAndOversizedPhotosAreRejected() {
        MockMultipartFile empty = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[0]);
        MockMultipartFile huge = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);

        assertThrows(IllegalArgumentException.class, () -> storage.saveIssueEvidence(empty));
        assertThrows(IllegalArgumentException.class, () -> storage.saveIssueEvidence(huge));
        assertThrows(IllegalArgumentException.class, () -> storage.saveIssueEvidence(null));
    }

    @Test
    void aFileNameThatEscapesTheFolderIsNeverServedOrDeleted() {
        assertNull(storage.loadIssueEvidence("../licenses/secret.jpg"));
        assertNull(storage.loadIssueEvidence(null));
        storage.deleteIssueEvidence("../licenses/secret.jpg");
    }
}
