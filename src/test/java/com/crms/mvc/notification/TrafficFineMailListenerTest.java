package com.crms.mvc.notification;

import com.crms.mvc.dto.TrafficFineNoticeDTO;
import com.crms.mvc.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.crms.mvc.event.TrafficFineRecordedEvent;
import com.crms.mvc.event.TrafficFineRemovedEvent;

/** The customer gets the police notice photo as an attachment; a removal email carries nothing. */
class TrafficFineMailListenerTest {
    private CustomerMailer mailer;
    private FileUploadService files;
    private TrafficFineMailListener listener;

    @BeforeEach
    void setUp() {
        mailer = mock(CustomerMailer.class);
        files = mock(FileUploadService.class);
        when(mailer.isConfigured()).thenReturn(true);
        when(mailer.url(any())).thenReturn("http://localhost:8081/bookings/6");
        listener = new TrafficFineMailListener(mailer, files);
    }

    @Test
    @SuppressWarnings("unchecked")
    void theRecordedEmailAttachesThePolicePhoto() throws Exception {
        Resource photo = new ByteArrayResource(new byte[]{1, 2, 3});
        when(files.loadTrafficFineImage("fine-abc.png")).thenReturn(photo);

        listener.onRecorded(new TrafficFineRecordedEvent(notice(), "fine-abc.png"));

        ArgumentCaptor<Map<String, Resource>> attachments = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(mailer).send(eq("a@example.com"), any(), eq("mail/traffic-fine-recorded"), variables.capture(), attachments.capture());
        assertThat(attachments.getValue()).containsOnlyKeys("bien-ban-phat-nguoi.png");
        assertThat(attachments.getValue().get("bien-ban-phat-nguoi.png")).isSameAs(photo);
        assertThat(variables.getValue()).containsEntry("hasAttachment", true);
        assertThat(variables.getValue()).containsEntry("attachmentName", "bien-ban-phat-nguoi.png");
    }

    @Test
    @SuppressWarnings("unchecked")
    void theEmailStillGoesOutWhenThePhotoCannotBeRead() throws Exception {
        when(files.loadTrafficFineImage(any())).thenThrow(new NoSuchElementException("missing"));

        listener.onRecorded(new TrafficFineRecordedEvent(notice(), "gone.jpg"));

        ArgumentCaptor<Map<String, Resource>> attachments = ArgumentCaptor.forClass(Map.class);
        verify(mailer).send(eq("a@example.com"), any(), eq("mail/traffic-fine-recorded"), any(), attachments.capture());
        assertThat(attachments.getValue()).isEmpty();
    }

    @Test
    void theRemovedEmailHasNoAttachment() throws Exception {
        listener.onRemoved(new TrafficFineRemovedEvent(notice()));

        verify(mailer).send(eq("a@example.com"), any(), eq("mail/traffic-fine-removed"), any(), eq(Map.of()));
        verify(files, never()).loadTrafficFineImage(any());
    }

    private TrafficFineNoticeDTO notice() {
        return new TrafficFineNoticeDTO(5L, 6L, "Nguyễn Văn A", "a@example.com", "51B-678.90",
                OffsetDateTime.now().minusHours(2), OffsetDateTime.now(), "Nhân viên",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().minusHours(1), "CTR-0001");
    }
}
