package com.crms.mvc.config;

import com.crms.mvc.interceptor.AuthInterceptor;
import com.crms.mvc.interceptor.RoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final RoleInterceptor roleInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, RoleInterceptor roleInterceptor) {
        this.authInterceptor = authInterceptor;
        this.roleInterceptor = roleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/profile/**", "/bookings/**", "/licenses/**");

        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/admin/**");
    }

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
