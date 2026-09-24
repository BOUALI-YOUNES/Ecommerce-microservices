package com.younes.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PayementApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayementApplication.class, args);
	}

}
