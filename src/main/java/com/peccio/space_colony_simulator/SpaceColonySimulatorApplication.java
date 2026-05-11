package com.peccio.space_colony_simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class SpaceColonySimulatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpaceColonySimulatorApplication.class, args);
	}

}
