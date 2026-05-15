package com.paymentservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.test.util.ReflectionTestUtils;

import com.paymentservice.config.PaymentMessageProducer;
import com.paymentservice.dto.PaymentVerificationRequest;
import com.paymentservice.entity.PaymentOrder;
import com.paymentservice.exception.GlobalExceptionHandler;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.resource.AdminPaymentController;
import com.paymentservice.resource.PaymentResource;
import com.paymentservice.service.RazorpayService;
class PaymentServiceCoverageTest {

	@Test
	void paymentResourceCoversConfigVerifyAndStatusPaths() throws Exception {
		RazorpayService razorpayService = org.mockito.Mockito.mock(RazorpayService.class);
		PaymentRepository paymentRepository = org.mockito.Mockito.mock(PaymentRepository.class);
		PaymentMessageProducer producer = org.mockito.Mockito.mock(PaymentMessageProducer.class);
		PaymentResource resource = new PaymentResource();

		ReflectionTestUtils.setField(resource, "razorpayKeyId", "rzp_test_key");
		ReflectionTestUtils.setField(resource, "paymentService", razorpayService);
		ReflectionTestUtils.setField(resource, "repository", paymentRepository);
		ReflectionTestUtils.setField(resource, "producer", producer);

		when(razorpayService.verifyPayment("order_1", "pay_1", "sig")).thenReturn(true);
		when(razorpayService.verifyPayment("bad", "pay_1", "sig")).thenReturn(false);

		PaymentOrder paymentOrder = PaymentOrder.builder().razorpayOrderId("order_1").userId("7").userEmail("a@example.com")
				.amount(99.0).status("PENDING").createdAt(LocalDateTime.now()).build();
		when(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(paymentOrder));
		when(paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc("7", "SUCCESS")).thenReturn(List.of(
				PaymentOrder.builder().userId("7").amount(99.0).status("SUCCESS").createdAt(LocalDateTime.now()).build()));
		when(paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc("9", "SUCCESS")).thenReturn(List.of());
		PaymentVerificationRequest successRequest = new PaymentVerificationRequest();
		successRequest.setRazorpay_order_id("order_1");
		successRequest.setRazorpay_payment_id("pay_1");
		successRequest.setRazorpay_signature("sig");
		PaymentVerificationRequest failureRequest = new PaymentVerificationRequest();
		failureRequest.setRazorpay_order_id("bad");
		failureRequest.setRazorpay_payment_id("pay_1");
		failureRequest.setRazorpay_signature("sig");

		assertEquals("rzp_test_key", ((Map<?, ?>) resource.getPaymentConfig().getBody()).get("keyId"));
		assertEquals("success", ((Map<?, ?>) resource.verify(successRequest).getBody()).get("status"));
		assertEquals(400, resource.verify(failureRequest).getStatusCode().value());
		assertEquals(Boolean.TRUE, ((Map<?, ?>) resource.getSubscriptionStatus("7").getBody()).get("isSubscribed"));
		assertEquals(Boolean.FALSE, ((Map<?, ?>) resource.getSubscriptionStatus("9").getBody()).get("isSubscribed"));
	}

	@Test
	void adminControllerAndExceptionHandlerCoverRemainingPaths() {
		PaymentRepository paymentRepository = org.mockito.Mockito.mock(PaymentRepository.class);
		AdminPaymentController adminPaymentController = new AdminPaymentController();
		ReflectionTestUtils.setField(adminPaymentController, "paymentRepository", paymentRepository);
		when(paymentRepository.findAll()).thenReturn(List.of(PaymentOrder.builder().userId("7").build()));

		assertEquals(1, adminPaymentController.getAllPayments().getBody().size());

		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		assertEquals("gateway_error",
				((Map<?, ?>) handler.handleRazorpayException(new com.razorpay.RazorpayException("down")).getBody()).get("status"));
		assertEquals("error", ((Map<?, ?>) handler.handleGeneralException(new Exception("x")).getBody()).get("status"));
	}

	@Test
	void applicationMainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			PaymentServiceApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(PaymentServiceApplication.class, new String[] { "test" }));
		}
	}
}
