package com.crms.mvc.config;

import com.crms.mvc.interceptor.AuthInterceptor;
import com.crms.mvc.interceptor.RoleInterceptor;
import com.crms.mvc.interceptor.SessionAccountInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final SessionAccountInterceptor sessionAccountInterceptor;
    private final AuthInterceptor authInterceptor;
    private final RoleInterceptor roleInterceptor;

    public WebConfig(SessionAccountInterceptor sessionAccountInterceptor,
                     AuthInterceptor authInterceptor,
                     RoleInterceptor roleInterceptor) {
        this.sessionAccountInterceptor = sessionAccountInterceptor;
        this.authInterceptor = authInterceptor;
        this.roleInterceptor = roleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionAccountInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/register", "/logout", "/error",
                        "/css/**", "/js/**", "/images/**", "/fonts/**", "/uploads/**");

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

        String promotionPath = Path.of("uploads", "promotions").toAbsolutePath().normalize().toUri().toString();
        if (!promotionPath.endsWith("/")) {
            promotionPath += "/";
        }

        registry.addResourceHandler("/uploads/promotions/**")
                .addResourceLocations(promotionPath);
    }
}
