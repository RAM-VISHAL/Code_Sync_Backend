package com.paymentservice.serviceTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.paymentservice.service.RazorpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.razorpay.RazorpayClient;

@ExtendWith(MockitoExtension.class)
class RazorpayServiceTest {

	@Mock
	private RazorpayClient razorpayClient;

	@InjectMocks
	private RazorpayService razorpayService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(razorpayService, "keySecret", "secret");
	}

	@Test
	void verifyPayment_invalidSignatureReturnsFalse() {
		boolean result = razorpayService.verifyPayment("order", "payment", "bad-signature");

		assertFalse(result);
	}
}
