package com.adminserver;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class AdminServerApplicationUnitTest {

	@Test
	void mainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			AdminServerApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(AdminServerApplication.class, new String[] { "test" }));
		}
	}
}
