package com.crms.mvc.notification;

import com.crms.mvc.dto.IssueNoticeDTO;
import com.crms.mvc.event.IssueCancelledEvent;
import com.crms.mvc.event.IssueFeeDueEvent;
import com.crms.mvc.event.IssueUpdatedEvent;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.IssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The customer is emailed what an Issue costs; a mail problem must never break the Issue change. */
class IssueNotificationListenerTest {
    private final IssueService issueService = mock(IssueService.class);
    private final FileUploadService fileUploadService = mock(FileUploadService.class);
    private final CustomerMailer mailer = mock(CustomerMailer.class);
    private IssueNotificationListener listener;

    @BeforeEach
    void setUp() {
        when(mailer.isConfigured()).thenReturn(true);
        when(mailer.url(any())).thenAnswer(inv -> "http://localhost:8081" + inv.getArgument(0));
        listener = new IssueNotificationListener(mailer, issueService, fileUploadService);
    }

    @Test
    void staysQuietWhenEmailIsNotConfigured() throws Exception {
        when(mailer.isConfigured()).thenReturn(false);

        listener.onIssueFeeDue(new IssueFeeDueEvent(7L));

        verifyNoInteractions(issueService);
        verify(mailer, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void emailsTheCustomerWhatIsOwedAndWhereToPay() throws Exception {
        when(issueService.findNotice(7L)).thenReturn(Optional.of(notice(List.of(), "800000")));

        listener.onIssueFeeDue(new IssueFeeDueEvent(7L));

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        verify(mailer).send(eq("an@example.com"), subject.capture(), eq("mail/issue-fee-due"), vars.capture(), any(Map.class));
        assertThat(subject.getValue()).contains("#42");
        assertThat(vars.getValue()).containsEntry("subject", "Red light, Le Loi street")
                .containsEntry("amount", new BigDecimal("800000"))
                .containsEntry("bookingUrl", "http://localhost:8081/bookings/42")
                .containsEntry("photoCount", 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void theEvidencePhotosAreAttachedToTheFirstNotice() throws Exception {
        when(issueService.findNotice(7L)).thenReturn(Optional.of(notice(List.of("issue-a.jpg", "issue-gone.png", "issue-b.png"), "200000")));
        when(fileUploadService.loadIssueEvidence("issue-a.jpg")).thenReturn(photo("issue-a.jpg"));
        when(fileUploadService.loadIssueEvidence("issue-gone.png")).thenThrow(new NoSuchElementException("missing"));
        when(fileUploadService.loadIssueEvidence("issue-b.png")).thenReturn(photo("issue-b.png"));

        listener.onIssueFeeDue(new IssueFeeDueEvent(7L));

        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Resource>> attachments = ArgumentCaptor.forClass(Map.class);
        verify(mailer).send(eq("an@example.com"), any(), eq("mail/issue-fee-due"), vars.capture(), attachments.capture());
        // a photo that cannot be read is skipped, the rest keep their order and are numbered without a gap
        assertThat(attachments.getValue()).containsOnlyKeys("anh-su-co-1.jpg", "anh-su-co-2.png");
        assertThat(attachments.getValue().keySet()).containsExactly("anh-su-co-1.jpg", "anh-su-co-2.png");
        assertThat(vars.getValue()).containsEntry("photoCount", 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void anUpdateEmailSaysWhatChanged() throws Exception {
        OffsetDateTime occurred = OffsetDateTime.now().minusDays(1);
        when(issueService.findNotice(7L)).thenReturn(Optional.of(new IssueNoticeDTO(
                7L, 42L, "Nguyễn Văn An", "an@example.com", "Hư hỏng", "Dented bumper",
                occurred, new BigDecimal("350000"), List.of())));

        listener.onIssueUpdated(new IssueUpdatedEvent(7L, "Hư hỏng", "Scratch", null, occurred, new BigDecimal("200000")));

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        verify(mailer).send(eq("an@example.com"), subject.capture(), eq("mail/issue-fee-updated"), vars.capture());
        assertThat(subject.getValue()).contains("Cập nhật").contains("#7");
        assertThat(vars.getValue()).containsEntry("amountChanged", true)
                .containsEntry("subjectChanged", true)
                .containsEntry("typeChanged", false)
                .containsEntry("occurredChanged", false)
                .containsEntry("previousAmount", new BigDecimal("200000"))
                .containsEntry("previousSubject", "Scratch")
                .containsEntry("amount", new BigDecimal("350000"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aCancelledIssueIsRetractedWithItsReason() throws Exception {
        when(issueService.findNotice(7L)).thenReturn(Optional.of(notice(List.of(), "200000")));

        listener.onIssueCancelled(new IssueCancelledEvent(7L, "Recorded on the wrong booking"));

        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        verify(mailer).send(eq("an@example.com"), any(), eq("mail/issue-cancelled"), vars.capture());
        assertThat(vars.getValue()).containsEntry("cancelReason", "Recorded on the wrong booking")
                .containsEntry("issueId", 7L);
    }

    @Test
    void aMailFailureIsSwallowedSoTheCallerIsNeverBroken() {
        when(issueService.findNotice(7L)).thenThrow(new IllegalStateException("db down"));

        listener.onIssueFeeDue(new IssueFeeDueEvent(7L));
    }

    @Test
    void aSendFailureIsSwallowedToo() throws Exception {
        when(issueService.findNotice(7L)).thenReturn(Optional.of(notice(List.of(), "200000")));
        doThrow(new IllegalStateException("smtp down")).when(mailer).send(any(), any(), any(), any(), any());

        listener.onIssueFeeDue(new IssueFeeDueEvent(7L));
    }

    private static IssueNoticeDTO notice(List<String> photos, String fee) {
        return new IssueNoticeDTO(7L, 42L, "Nguyễn Văn An", "an@example.com", "Phạt nguội", "Red light, Le Loi street",
                OffsetDateTime.now().minusDays(20), new BigDecimal(fee), photos);
    }

    private static Resource photo(String name) {
        return new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }
}
