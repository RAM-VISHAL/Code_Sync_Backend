package com.paymentservice.resource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentservice.config.PaymentMessageProducer;
import com.paymentservice.dto.PaymentVerificationRequest;
import com.paymentservice.entity.PaymentOrder;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.service.RazorpayService;
import com.razorpay.Order;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentResource {

	@Value("${razorpay.key.id}")
	private String razorpayKeyId;

	@Autowired
	private RazorpayService paymentService;

	@Autowired
	private PaymentRepository repository;

	@Autowired
	private PaymentMessageProducer producer;

	@GetMapping("/config")
	public ResponseEntity<?> getPaymentConfig() {
		return ResponseEntity.ok(Map.of("keyId", razorpayKeyId));
	}

	@PostMapping("/create-order")
	public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) throws Exception {
		Double amount = Double.parseDouble(data.get("amount").toString());
		String userId = data.get("userId").toString();
		String email = data.get("email").toString();

		Order order = paymentService.createRazorpayOrder(amount, userId, email);

		// Persist the order immediately as PENDING
		PaymentOrder paymentOrder = PaymentOrder.builder().razorpayOrderId(order.get("id")).amount(amount)
				.userId(userId).userEmail(email).status("PENDING").build();

		repository.save(paymentOrder);
		try {
			producer.sendPaymentStatusNotification(paymentOrder.getUserId(), paymentOrder.getUserEmail(),
					paymentOrder.getRazorpayOrderId(), paymentOrder.getAmount(), "PENDING");
		} catch (Exception e) {
			System.err.println("Failed to send pending payment notification: " + e.getMessage());
		}
		return ResponseEntity.ok(order.toJson().toMap());
	}

	@PostMapping("/verify")
	public ResponseEntity<?> verify(@RequestBody PaymentVerificationRequest request) {
		boolean isValid = paymentService.verifyPayment(request.getRazorpay_order_id(), request.getRazorpay_payment_id(),
				request.getRazorpay_signature());

		if (isValid) {
			return repository.findByRazorpayOrderId(request.getRazorpay_order_id()).map(p -> {
				p.setStatus("SUCCESS");
				repository.save(p);

				// Async push to Notification-Service via RabbitMQ (Wrapped in try-catch for resilience)
				try {
					producer.sendPaymentStatusNotification(p.getUserId(), p.getUserEmail(), p.getRazorpayOrderId(),
							p.getAmount(), "SUCCESS");
				} catch (Exception e) {
					System.err.println("Failed to send payment notification: " + e.getMessage());
				}
				return ResponseEntity.ok(Map.of("status", "success"));
			}).orElse(ResponseEntity.status(404).body(Map.of("message", "Order record not found")));
		}

		return ResponseEntity.status(400).body(Map.of("status", "failure", "message", "Invalid payment signature"));
	}

	// Verifies active subscription status and returns billing details
	@GetMapping("/status/{userId}")
	public ResponseEntity<?> getSubscriptionStatus(@PathVariable String userId) {

		// Fetch the most recent successful payment
		List<PaymentOrder> orders = repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "SUCCESS");

		if (orders.isEmpty()) {
			Map<String, Object> response = new HashMap<>();
			response.put("isSubscribed", false);
			return ResponseEntity.ok(response);
		}

		PaymentOrder latestPayment = orders.get(0);

		// Calculate Expiry 30 days from purchase date
		LocalDateTime expiryDate = latestPayment.getCreatedAt().plusDays(30);
		boolean isActive = LocalDateTime.now().isBefore(expiryDate);

		// Safely build the response map using Strings for Dates to prevent Jackson
		// Serialization crashes
		Map<String, Object> response = new HashMap<>();
		response.put("isSubscribed", isActive);
		response.put("purchaseDate",
				latestPayment.getCreatedAt() != null ? latestPayment.getCreatedAt().toString() : null);
		response.put("expiryDate", expiryDate.toString());
		response.put("amount", latestPayment.getAmount());

		return ResponseEntity.ok(response);
	}
}
