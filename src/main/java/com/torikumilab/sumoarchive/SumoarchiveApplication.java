package com.torikumilab.sumoarchive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SumoarchiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(SumoarchiveApplication.class, args);
	}

}
