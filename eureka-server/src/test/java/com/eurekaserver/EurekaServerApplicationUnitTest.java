package com.eurekaserver;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class EurekaServerApplicationUnitTest {

	@Test
	void mainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			EurekaServerApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(EurekaServerApplication.class, new String[] { "test" }));
		}
	}
}
