package com.crms.mvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables asynchronous notification delivery; Spring Boot supplies the default thread pool.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
