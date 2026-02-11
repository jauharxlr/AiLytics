package com.ailytics.ailytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AilyticsApplication {

	public static void main(String[] args) {
		SpringApplication.run(AilyticsApplication.class, args);
	}

}
