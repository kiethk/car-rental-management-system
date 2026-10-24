package com.crms.mvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Only vehicle photos are public. License images and contract PDFs stay private and are
        // served by controllers that check ownership first.
        String vehiclePath = Path.of("uploads", "vehicles").toAbsolutePath().normalize().toUri().toString();
        if (!vehiclePath.endsWith("/")) {
            vehiclePath += "/";
        }

        registry.addResourceHandler("/uploads/vehicles/**")
                .addResourceLocations(vehiclePath);
    }
}
