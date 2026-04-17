package com.guidewire.in;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GuidewirebackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuidewirebackendApplication.class, args);
		System.out.println("Project is running...");
	}

}
