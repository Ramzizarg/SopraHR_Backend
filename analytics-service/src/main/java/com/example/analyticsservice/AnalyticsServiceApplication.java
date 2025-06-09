package com.example.analyticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Analytics Service Application
 * Provides comprehensive analytics and metrics for the admin dashboard
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AnalyticsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnalyticsServiceApplication.class, args);
	}
	
	/**
	 * Configure WebClient.Builder for making reactive HTTP requests to other microservices
	 */
	@Bean
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}
}
