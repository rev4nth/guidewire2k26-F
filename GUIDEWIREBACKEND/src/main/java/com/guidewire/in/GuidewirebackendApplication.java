package com.guidewire.in;

import org.hibernate.internal.build.AllowSysOut;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GuidewirebackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuidewirebackendApplication.class, args);
		System.out.println("Project is running...");
	}

}
