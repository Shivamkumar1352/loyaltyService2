package com.loyaltyService.wallet_service.controller;

import com.loyaltyService.wallet_service.dto.PaymentVerifyRequest;
import com.loyaltyService.wallet_service.entity.Payment;
import com.loyaltyService.wallet_service.repository.PaymentRepository;
import com.loyaltyService.wallet_service.service.WalletCommandService;
import com.loyaltyService.wallet_service.service.WalletQueryService;
import com.loyaltyService.wallet_service.service.RazorpayService;
import com.razorpay.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.MessageDigest;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

        private final PaymentRepository paymentRepo;
        private final RazorpayService razorpayService;


        @Value("${razorpay.secret}")
        private String secret;

        @PostMapping("/create-order")
        public ResponseEntity<?> createOrder(
                        @RequestHeader("X-User-Id") Long userId,
                        @RequestParam BigDecimal amount) throws Exception {

                Order order = razorpayService.createOrder(userId, amount);
                return ResponseEntity.ok(Map.of(
                                "orderId", order.get("id"),
                                "amount(paise)", order.get("amount"),
                                "currency", order.get("currency")));
        }

        @PostMapping("/verify")
        public ResponseEntity<?> verify(
                @RequestHeader("X-User-Id") Long userId,
                @RequestBody PaymentVerifyRequest request) {

                try {
                        razorpayService.verifyPayment(userId, request);
                        return ResponseEntity.ok("Payment verified & wallet credited");
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(e.getMessage());
                }
        }
}