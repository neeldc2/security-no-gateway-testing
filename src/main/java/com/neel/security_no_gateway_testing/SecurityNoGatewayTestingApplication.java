package com.neel.security_no_gateway_testing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@EnableJms
@SpringBootApplication
public class SecurityNoGatewayTestingApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityNoGatewayTestingApplication.class, args);
	}

}
