package com.loyaltyService.wallet_service.service.impl;

import com.loyaltyService.wallet_service.dto.PaymentFailureRequest;
import com.loyaltyService.wallet_service.dto.PaymentVerifyRequest;
import com.loyaltyService.wallet_service.entity.Payment;
import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.repository.PaymentRepository;
import com.loyaltyService.wallet_service.repository.TransactionRepository;
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
import org.springframework.transaction.annotation.Transactional;
import com.razorpay.Utils;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RazorpayServiceImpl implements RazorpayService {

    private final PaymentRepository paymentRepo;
    private final TransactionRepository transactionRepo;
    private final WalletCommandService walletCommandService;
    private final WalletQueryService walletQueryService;
    private final com.loyaltyService.wallet_service.service.KafkaProducerService kafkaProducer;

    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;

    @Override
    @Transactional
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
                        .status("PENDING")
                        .build()
        );

        transactionRepo.save(Transaction.builder()
                .receiverId(userId)
                .amount(amount)
                .status(Transaction.TxnStatus.PENDING)
                .type(Transaction.TxnType.TOPUP)
                .referenceId(orderId)
                .idempotencyKey(orderId)
                .description("Razorpay wallet top-up initiated")
                .build());

        return order;
    }

    @Override
    @Transactional
    public void verifyPayment(Long userId, PaymentVerifyRequest req) throws RazorpayException {
        Payment payment = paymentRepo.findById(req.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Transaction transaction = transactionRepo.findByReferenceId(req.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!payment.getUserId().equals(userId)) {
            markFailed(payment, transaction, req.getRazorpayPaymentId());
            throw new RuntimeException("Payment does not belong to this user");
        }

        if (Transaction.TxnStatus.SUCCESS.equals(transaction.getStatus()) || "SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("Payment already processed");
        }

        if (Transaction.TxnStatus.FAILED.equals(transaction.getStatus()) || "FAILED".equals(payment.getStatus())) {
            throw new RuntimeException("Payment already marked failed");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", req.getRazorpayOrderId());
            options.put("razorpay_payment_id", req.getRazorpayPaymentId());
            options.put("razorpay_signature", req.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, secret);
            if (!isValid) {
                markFailed(payment, transaction, req.getRazorpayPaymentId());
                throw new RuntimeException("Invalid signature");
            }

            BigDecimal amount = payment.getAmount();

            walletCommandService.topup(userId, amount, payment.getOrderId());

            payment.setPaymentId(req.getRazorpayPaymentId());
            payment.setStatus("SUCCESS");
            paymentRepo.save(payment);

            transaction.setStatus(Transaction.TxnStatus.SUCCESS);
            transaction.setDescription("Razorpay wallet top-up completed");
            transactionRepo.save(transaction);

            BigDecimal updatedBalance = walletQueryService.getBalance(userId).getBalance();

            kafkaProducer.send("payment-events", Map.of(
                    "event", "PAYMENT_SUCCESS",
                    "userId", payment.getUserId(),
                    "amount", payment.getAmount(),
                    "orderId", payment.getOrderId(),
                    "paymentId", req.getRazorpayPaymentId(),
                    "balance", updatedBalance
            ));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            markFailed(payment, transaction, req.getRazorpayPaymentId());
            throw new RuntimeException("Payment verification failed", ex);
        }
    }

    @Override
    @Transactional
    public void markPaymentFailed(Long userId, PaymentFailureRequest req) {
        Payment payment = paymentRepo.findById(req.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Transaction transaction = transactionRepo.findByReferenceId(req.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("Payment does not belong to this user");
        }

        if (Transaction.TxnStatus.SUCCESS.equals(transaction.getStatus()) || "SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("Payment already processed");
        }

        if (Transaction.TxnStatus.FAILED.equals(transaction.getStatus()) || "FAILED".equals(payment.getStatus())) {
            return;
        }

        markFailed(payment, transaction, req.getRazorpayPaymentId());
    }

    private void markFailed(Payment payment, Transaction transaction, String paymentId) {
        payment.setPaymentId(paymentId);
        payment.setStatus("FAILED");
        paymentRepo.save(payment);

        transaction.setStatus(Transaction.TxnStatus.FAILED);
        transaction.setDescription("Razorpay wallet top-up failed");
        transactionRepo.save(transaction);
    }
}
