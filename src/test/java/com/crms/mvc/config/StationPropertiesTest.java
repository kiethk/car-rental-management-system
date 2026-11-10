package com.crms.mvc.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/** The depot is configured in one place; nothing configured means the FPT campus. */
class StationPropertiesTest {
    @Configuration
    @EnableConfigurationProperties(StationProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void withNothingConfiguredItIsTheFptCampus() {
        runner.run(context -> assertThat(context.getBean(StationProperties.class)).isEqualTo(StationProperties.DEFAULT));
    }

    @Test
    void aSingleOverrideKeepsTheOtherDefaults() {
        runner.withPropertyValues("crms.station.address=1 Test Street").run(context -> {
            StationProperties station = context.getBean(StationProperties.class);
            assertThat(station.address()).isEqualTo("1 Test Street");
            assertThat(station.name()).isEqualTo(StationProperties.DEFAULT_NAME);
            assertThat(station.phone()).isEqualTo(StationProperties.DEFAULT_PHONE);
            assertThat(station.mapEmbedUrl()).isEqualTo(StationProperties.DEFAULT_MAP_EMBED_URL);
        });
    }

    @Test
    void theShippedApplicationPropertiesAgreeWithTheDefaults() throws Exception {
        Properties shipped = PropertiesLoaderUtils.loadProperties(new FileSystemResource("src/main/resources/application.properties"));

        assertThat(shipped.getProperty("crms.station.name")).isEqualTo(StationProperties.DEFAULT_NAME);
        assertThat(shipped.getProperty("crms.station.address")).isEqualTo(StationProperties.DEFAULT_ADDRESS);
        assertThat(shipped.getProperty("crms.station.phone")).isEqualTo(StationProperties.DEFAULT_PHONE);
        assertThat(shipped.getProperty("crms.station.map-embed-url")).isEqualTo(StationProperties.DEFAULT_MAP_EMBED_URL);
    }
}
