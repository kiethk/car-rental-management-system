package com.crms.mvc.dto;

import com.crms.mvc.enums.SlotStatus;
import com.crms.mvc.enums.IssueStatus;
import com.crms.mvc.enums.IssueType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the admin issue list asks for: filters that fall back to "everything" instead of failing on bad input. */
class IssueQueryTest {

    @Test
    void unknownStatusOrTypeMeansNoFilter() {
        IssueQueryDTO query = new IssueQueryDTO();
        assertEquals("all", query.getStatus());
        assertNull(query.getIssueStatus());
        assertNull(query.getIssueType());

        query.setStatus("bogus");
        query.setType("bogus");

        assertEquals("all", query.getStatus());
        assertNull(query.getIssueStatus());
        assertEquals("all", query.getType());
        assertNull(query.getIssueType());
    }

    @Test
    void statusAndTypeAreRecognisedWhateverTheirCase() {
        IssueQueryDTO query = new IssueQueryDTO();

        query.setStatus(" Resolved ");
        query.setType("damage");

        assertEquals("resolved", query.getStatus());
        assertEquals(IssueStatus.RESOLVED, query.getIssueStatus());
        assertEquals("DAMAGE", query.getType());
        assertEquals(IssueType.DAMAGE, query.getIssueType());
    }

    @Test
    void thePlateIsTrimmedAndSearchedAsAnyCaseContainsPattern() {
        IssueQueryDTO query = new IssueQueryDTO();
        assertNull(query.getPlate());
        assertNull(query.getPlatePattern());

        query.setPlate("   ");
        assertNull(query.getPlate());

        query.setPlate("  51A-12 ");
        assertEquals("51A-12", query.getPlate());
        assertEquals("%51a-12%", query.getPlatePattern());
    }

    @Test
    void likeWildcardsTypedInThePlateSearchForThemselves() {
        IssueQueryDTO query = new IssueQueryDTO();
        query.setPlate("5%_1");

        assertEquals("%5\\%\\_1%", query.getPlatePattern());
    }

    @Test
    void theDateRangeCoversWholeDaysInVietnamTime() {
        IssueQueryDTO query = new IssueQueryDTO();
        assertNull(query.getFromInstant());
        assertNull(query.getToInstant());

        query.setFrom(LocalDate.of(2026, 10, 1));
        query.setTo(LocalDate.of(2026, 10, 31));

        assertEquals(OffsetDateTime.parse("2026-10-01T00:00:00+07:00"), query.getFromInstant());
        // the end day is included, so the bound is the start of the day after it
        assertEquals(OffsetDateTime.parse("2026-11-01T00:00:00+07:00"), query.getToInstant());
    }

    @Test
    void theUrlKeepsEveryFilterAndPage() {
        IssueQueryDTO query = new IssueQueryDTO();
        query.setStatus("open");
        query.setType("DAMAGE");
        query.setPlate("51A");
        query.setBookingId(42L);
        query.setFrom(LocalDate.of(2026, 10, 1));
        query.setTo(LocalDate.of(2026, 10, 31));
        query.setSize(20);

        String url = query.urlForPage("/admin/issues", 2);

        assertTrue(url.startsWith("/admin/issues?"), url);
        for (String part : new String[]{"status=open", "type=DAMAGE", "plate=51A", "bookingId=42",
                "from=2026-10-01", "to=2026-10-31", "size=20", "page=2"}) {
            assertTrue(url.contains(part), part + " missing from " + url);
        }
    }

    @Test
    void theUrlOmitsFiltersThatAreNotSet() {
        String url = new IssueQueryDTO().urlForPage("/admin/issues", 1);

        assertTrue(url.contains("status=all"), url);
        assertTrue(!url.contains("plate="), url);
        assertTrue(!url.contains("bookingId="), url);
        assertTrue(!url.contains("from="), url);
    }
}
