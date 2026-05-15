package com.paymentservice.serviceTest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.paymentservice.config.PaymentMessageProducer;
import com.paymentservice.entity.PaymentOrder;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.resource.PaymentResource;
import com.paymentservice.service.RazorpayService;

class PaymentResourceTest {

	private MockMvc mockMvc;

	@Mock
	private RazorpayService paymentService;

	@Mock
	private PaymentRepository repository;

	@Mock
	private PaymentMessageProducer producer;

	@InjectMocks
	private PaymentResource paymentResource;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(paymentResource).build();
	}

	@Test
	void testVerifyPayment_Success() throws Exception {
		when(paymentService.verifyPayment(anyString(), anyString(), anyString())).thenReturn(true);

		PaymentOrder mockOrder = PaymentOrder.builder().razorpayOrderId("order_123").userId("101")
				.userEmail("abhay@example.com").amount(499.0).build();

		when(repository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(mockOrder));

		String requestBody = """
				{
				    "razorpay_order_id": "order_123",
				    "razorpay_payment_id": "pay_123",
				    "razorpay_signature": "sig_123"
				}
				""";

		mockMvc.perform(post("/api/v1/payments/verify").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));

		verify(producer, times(1)).sendPaymentStatusNotification(anyString(), anyString(), anyString(),
				org.mockito.ArgumentMatchers.anyDouble(), anyString());
	}
}
