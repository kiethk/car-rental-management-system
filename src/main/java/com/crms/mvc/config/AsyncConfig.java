package com.crms.mvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables @Async; Spring Boot supplies the default thread pool (spring.task.execution.*).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
