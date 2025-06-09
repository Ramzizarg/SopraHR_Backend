package com.example.planningservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PlanningServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanningServiceApplication.class, args);
	}

}
