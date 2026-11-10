package com.crms.mvc;
import org.springframework.boot.SpringApplication;
import com.crms.mvc.config.StationProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import java.util.TimeZone;
@SpringBootApplication
@EnableConfigurationProperties(StationProperties.class)
public class CarRentalApplication {
    public static void main(String[] args) {
        // Use a portable JDBC timezone; Windows may report the legacy Asia/Saigon alias.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(CarRentalApplication.class, args);
    }
}
