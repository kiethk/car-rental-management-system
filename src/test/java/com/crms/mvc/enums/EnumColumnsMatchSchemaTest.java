package com.crms.mvc.enums;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Status and type columns are stored by the enum's name, and the database CHECKs list those names. Whenever an enum
 * and its CHECK drift apart, saving or loading breaks, so every CHECK ... IN list must equal one enum exactly.
 */
class EnumColumnsMatchSchemaTest {

    private static final List<Class<? extends Enum<?>>> ENUMS = List.of(
            BookingStatus.class, ContractTemplateStatus.class, DiscountType.class, EmployeeRole.class, FeeType.class,
            FuelType.class, HandoverType.class, InspectionStatus.class, IssueStatus.class, IssueType.class,
            PaymentMethod.class, PaymentStatus.class, PaymentType.class, SlotStatus.class, Transmission.class,
            VehicleCategory.class, VehicleStatus.class);

    @Test
    void everyCheckInListOfTheSchemaIsExactlyOneEnum() throws IOException {
        String schema = Files.readString(Path.of("database/schema.sql"));
        Matcher check = Pattern.compile("CHECK \\((\\w+) IN \\(([^)]*)\\)\\)").matcher(schema);
        int checks = 0;
        while (check.find()) {
            checks++;
            Set<String> allowed = Arrays.stream(check.group(2).split(","))
                    .map(v -> v.trim().replace("'", "")).collect(Collectors.toSet());
            boolean matchesAnEnum = ENUMS.stream().anyMatch(e -> names(e).equals(allowed));
            assertThat(matchesAnEnum).as("CHECK on column %s allows %s", check.group(1), allowed).isTrue();
        }
        assertThat(checks).isGreaterThan(0);
    }

    @Test
    void everyEnumValueIsAllowedBySomeCheckOfTheSchema() throws IOException {
        String schema = Files.readString(Path.of("database/schema.sql"));
        for (Class<? extends Enum<?>> type : ENUMS) {
            for (String name : names(type)) {
                assertThat(schema).as("%s value %s in schema.sql", type.getSimpleName(), name).contains("'" + name + "'");
            }
        }
    }

    private static Set<String> names(Class<? extends Enum<?>> type) {
        Set<String> names = new HashSet<>();
        for (Enum<?> value : type.getEnumConstants()) {
            names.add(value.name());
        }
        return names;
    }
}
