package com.portalcomunitario.mscommunity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsCommunityApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsCommunityApplication.class, args);
	}
}
