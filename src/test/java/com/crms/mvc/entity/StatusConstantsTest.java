package com.crms.mvc.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Booking and vehicle statuses are stored as text (queries, templates and the database CHECKs read these exact
 * values), so the constants must keep their values, and code must use the constants instead of retyping the strings.
 */
class StatusConstantsTest {

    @Test
    void bookingStatusValuesMatchWhatTheDatabaseAndTemplatesUse() {
        assertThat(Booking.STATUS_PENDING_PAYMENT).isEqualTo("pending_payment");
        assertThat(Booking.STATUS_CONFIRMED).isEqualTo("confirmed");
        assertThat(Booking.STATUS_ACTIVE).isEqualTo("active");
        assertThat(Booking.STATUS_COMPLETED).isEqualTo("completed");
        assertThat(Booking.STATUS_CANCELLED).isEqualTo("cancelled");
    }

    @Test
    void vehicleStatusValuesMatchWhatTheDatabaseAndTemplatesUse() {
        assertThat(Vehicle.STATUS_AVAILABLE).isEqualTo("available");
        assertThat(Vehicle.STATUS_RENTED).isEqualTo("rented");
        assertThat(Vehicle.STATUS_MAINTENANCE).isEqualTo("maintenance");
        assertThat(Vehicle.STATUS_RETIRED).isEqualTo("retired");
    }

    @Test
    void theSchemaAllowsExactlyTheBookingStatusesOfTheConstants() throws IOException {
        String schema = Files.readString(Path.of("database/schema.sql"));
        Matcher check = Pattern.compile("CHECK \\(status IN \\(([^)]*)\\)\\)").matcher(schema);
        boolean found = false;
        while (check.find()) {
            if (check.group(1).contains("pending_payment")) {
                found = true;
                assertThat(check.group(1)).contains("'" + Booking.STATUS_PENDING_PAYMENT + "'", "'" + Booking.STATUS_CONFIRMED + "'",
                        "'" + Booking.STATUS_ACTIVE + "'", "'" + Booking.STATUS_COMPLETED + "'", "'" + Booking.STATUS_CANCELLED + "'");
            }
        }
        assertThat(found).as("bookings.status CHECK constraint in schema.sql").isTrue();
    }

    @Test
    void noCodeRetypesAStatusStringInsteadOfUsingTheConstant() throws IOException {
        Pattern retyped = Pattern.compile("\"(pending_payment|confirmed|active|completed|cancelled|rented|maintenance|retired|available)\""
                + "\\s*\\.equalsIgnoreCase\\(|\\.setStatus\\(\"(pending_payment|confirmed|active|completed|cancelled|rented|maintenance|retired|available)\"\\)");
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            List<String> offenders = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().replace('\\', '/').contains("/entity/"))
                    .filter(p -> {
                        try {
                            return retyped.matcher(Files.readString(p)).find();
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .map(Path::toString)
                    .toList();
            assertThat(offenders).as("files that compare or assign a literal status").isEmpty();
        }
    }
}
