package com.crms.mvc.util;

import com.crms.mvc.dto.RentalDuration;
import com.crms.mvc.dto.RentalWindow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** The several spellings of the rental window in the query string, and what each falls back to. */
class RentalWindowParserTest {
    private static final LocalDate TOMORROW = LocalDate.now(DateTimeUtils.VIETNAM_ZONE).plusDays(1);

    @Test
    void nothingGivenMeansTomorrowNineToSix() {
        RentalWindow window = RentalWindowParser.parse(null, null, null, null, null);

        assertThat(window.start()).isEqualTo(TOMORROW.atTime(9, 0));
        assertThat(window.end()).isEqualTo(TOMORROW.atTime(18, 0));
    }

    @Test
    void blankValuesAreTreatedAsMissing() {
        RentalWindow window = RentalWindowParser.parse(" ", "", "  ", "", " ");

        assertThat(window.start()).isEqualTo(TOMORROW.atTime(9, 0));
        assertThat(window.end()).isEqualTo(TOMORROW.atTime(18, 0));
    }

    @Test
    void separateDatesAndTimesAreCombined() {
        RentalWindow window = RentalWindowParser.parse("2030-01-10", "2030-01-12", null, "08:00", "20:30");

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2030, 1, 10, 8, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2030, 1, 12, 20, 30));
    }

    @Test
    void aStartDateAloneEndsTheSameDay() {
        RentalWindow window = RentalWindowParser.parse("2030-03-05", null, null, null, null);

        assertThat(window.startDate()).isEqualTo(LocalDate.of(2030, 3, 5));
        assertThat(window.endDate()).isEqualTo(LocalDate.of(2030, 3, 5));
    }

    @Test
    void theLegacySingleDateSetsBothDays() {
        RentalWindow window = RentalWindowParser.parse(null, null, "2030-02-01", "10:00", "12:00");

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2030, 2, 1, 10, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2030, 2, 1, 12, 0));
    }

    @Test
    void startAndEndDatesWinOverTheLegacyDate() {
        RentalWindow window = RentalWindowParser.parse("2030-04-01", "2030-04-03", "2030-02-01", null, null);

        assertThat(window.startDate()).isEqualTo(LocalDate.of(2030, 4, 1));
        assertThat(window.endDate()).isEqualTo(LocalDate.of(2030, 4, 3));
    }

    @Test
    void fullDateTimeValuesInTheTimeParametersWinOverTheDates() {
        RentalWindow window = RentalWindowParser.parse("2030-01-01", null, null, "2030-04-02T10:00", "2030-04-03T12:00");

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2030, 4, 2, 10, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2030, 4, 3, 12, 0));
    }

    @Test
    void eachEndIsReadOnItsOwn() {
        // only the start is a full date-time: the end still comes from the dates and its own time of day
        RentalWindow window = RentalWindowParser.parse("2030-05-01", "2030-05-02", null, "2030-05-01T07:00", "21:00");

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2030, 5, 1, 7, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2030, 5, 2, 21, 0));
    }

    @Test
    void unreadableValuesFallBackInsteadOfFailing() {
        RentalWindow window = RentalWindowParser.parse("not-a-date", "31/12/2030", null, "nine", "2030-99-99T25:00");

        assertThat(window.start()).isEqualTo(TOMORROW.atTime(9, 0));
        assertThat(window.end()).isEqualTo(TOMORROW.atTime(18, 0));
    }

    @Test
    void surroundingSpacesAreIgnored() {
        RentalWindow window = RentalWindowParser.parse(" 2030-06-01 ", null, null, " 08:00 ", " 17:00 ");

        assertThat(window.start()).isEqualTo(LocalDateTime.of(2030, 6, 1, 8, 0));
        assertThat(window.end()).isEqualTo(LocalDateTime.of(2030, 6, 1, 17, 0));
    }

    @Test
    void theWindowKnowsItsOffsetMomentsAndLength() {
        RentalWindow window = RentalWindowParser.parse("2030-01-10", "2030-01-12", null, "08:00", "20:00");

        assertThat(window.startAt().getOffset()).isEqualTo(DateTimeUtils.VIETNAM_OFFSET);
        assertThat(window.startAt().toLocalDateTime()).isEqualTo(window.start());
        assertThat(window.duration()).isEqualTo(new RentalDuration(60));
    }

    @Test
    void anEmptyOrBackwardsWindowStillHasAtLeastOneHour() {
        assertThat(new RentalWindow(LocalDateTime.of(2030, 1, 1, 9, 0), LocalDateTime.of(2030, 1, 1, 9, 0)).duration().hours()).isEqualTo(1);
        assertThat(new RentalWindow(LocalDateTime.of(2030, 1, 1, 9, 0), LocalDateTime.of(2030, 1, 1, 8, 0)).duration().hours()).isEqualTo(1);
    }

    @Test
    void durationsAreShownAsDaysAndHours() {
        assertThat(new RentalDuration(1).display()).isEqualTo("1h");
        assertThat(new RentalDuration(9).display()).isEqualTo("9h");
        assertThat(new RentalDuration(27).display()).isEqualTo("1d 3h");
        assertThat(new RentalDuration(60).display()).isEqualTo("2d 12h");
        assertThat(new RentalDuration(48).display()).isEqualTo("2d ");
        assertThat(new RentalDuration(60).days()).isEqualTo(2);
        assertThat(new RentalDuration(60).remainingHours()).isEqualTo(12);
    }
}
