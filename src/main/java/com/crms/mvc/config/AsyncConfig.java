package com.crms.mvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Async and @EnableScheduling; Spring Boot supplies default thread pools.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
