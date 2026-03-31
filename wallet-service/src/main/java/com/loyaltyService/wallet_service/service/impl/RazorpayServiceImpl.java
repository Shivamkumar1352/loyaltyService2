package com.loyaltyService.wallet_service.service.impl;

import com.loyaltyService.wallet_service.dto.PaymentVerifyRequest;
import com.loyaltyService.wallet_service.entity.Payment;
import com.loyaltyService.wallet_service.repository.PaymentRepository;
import com.loyaltyService.wallet_service.service.RazorpayService;
import com.loyaltyService.wallet_service.service.WalletCommandService;
import com.loyaltyService.wallet_service.service.WalletQueryService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.razorpay.Utils;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RazorpayServiceImpl implements RazorpayService {

    private final PaymentRepository paymentRepo;
    private final WalletCommandService walletCommandService;
    private final WalletQueryService walletQueryService;
    private final com.loyaltyService.wallet_service.service.KafkaProducerService kafkaProducer;

    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;

    @Override
    public Order createOrder(Long userId, BigDecimal amount) throws RazorpayException {

        RazorpayClient client = new RazorpayClient(key, secret);

        JSONObject options = new JSONObject();
        options.put("amount", amount.multiply(BigDecimal.valueOf(100))); // paise
        options.put("currency", "INR");
        options.put("receipt", "wallet_" + System.currentTimeMillis());

        // ✅ CREATE ORDER FIRST
        Order order = client.orders.create(options);

        String orderId = order.get("id");
        // ✅ SAVE IN DB
        paymentRepo.save(
                Payment.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .amount(amount)
                        .status("CREATED")
                        .build()
        );

        return order;
    }
    @Override
    public void verifyPayment(Long userId, PaymentVerifyRequest req) throws RazorpayException {

        // ✅ VERIFY SIGNATURE (OFFICIAL WAY)
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", req.getRazorpayOrderId());
        options.put("razorpay_payment_id", req.getRazorpayPaymentId());
        options.put("razorpay_signature", req.getRazorpaySignature());

        boolean isValid = Utils.verifyPaymentSignature(options, secret);

        if (!isValid) {
            throw new RuntimeException("Invalid signature");
        }

        // ✅ FETCH PAYMENT
        Payment payment = paymentRepo.findById(req.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if ("SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("Payment already processed");
        }

        BigDecimal amount = payment.getAmount();

        // ✅ CREDIT WALLET
        walletCommandService.topup(userId, amount, payment.getOrderId());

        // ✅ UPDATE STATUS
        payment.setStatus("SUCCESS");
        paymentRepo.save(payment);

        // ✅ GET UPDATED BALANCE
        BigDecimal updatedBalance = walletQueryService.getBalance(userId).getBalance();

        // ✅ SEND KAFKA EVENT
        kafkaProducer.send("payment-events", Map.of(
                "event", "PAYMENT_SUCCESS",
                "userId", payment.getUserId(),
                "amount", payment.getAmount(),
                "orderId", payment.getOrderId(),
                "balance", updatedBalance
        ));
    }
}