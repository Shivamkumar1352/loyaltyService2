package com.loyaltyService.wallet_service.service;

import com.loyaltyService.wallet_service.dto.PaymentFailureRequest;
import com.loyaltyService.wallet_service.entity.Payment;
import com.loyaltyService.wallet_service.entity.Transaction;
import com.loyaltyService.wallet_service.repository.TransactionRepository;
import com.loyaltyService.wallet_service.repository.PaymentRepository;
import com.loyaltyService.wallet_service.service.KafkaProducerService;
import com.loyaltyService.wallet_service.service.WalletCommandService;
import com.loyaltyService.wallet_service.service.WalletQueryService;
import com.loyaltyService.wallet_service.service.impl.RazorpayServiceImpl;
import com.loyaltyService.wallet_service.dto.PaymentVerifyRequest;
import com.loyaltyService.wallet_service.dto.WalletBalanceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RazorpayServiceTest {

    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private TransactionRepository transactionRepo;
    @Mock
    private WalletCommandService walletCommandService;
    @Mock
    private WalletQueryService walletQueryService;
    @Mock
    private KafkaProducerService kafkaProducer;

    @InjectMocks
    private RazorpayServiceImpl razorpayService;

    @Test
    void createOrderWithoutCredentialsThrows() {
        ReflectionTestUtils.setField(razorpayService, "key", null);
        ReflectionTestUtils.setField(razorpayService, "secret", null);

        assertThrows(Exception.class, () -> razorpayService.createOrder(1L, new BigDecimal("100.00")));
    }

    @Test
    void verifyPaymentSuccessMarksPaymentAndTransactionSuccessful() throws Exception {
        ReflectionTestUtils.setField(razorpayService, "secret", "test-secret");

        Payment payment = Payment.builder()
                .orderId("order_123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .status("PENDING")
                .build();
        Transaction transaction = Transaction.builder()
                .receiverId(1L)
                .amount(new BigDecimal("100.00"))
                .status(Transaction.TxnStatus.PENDING)
                .type(Transaction.TxnType.TOPUP)
                .referenceId("order_123")
                .idempotencyKey("order_123")
                .build();
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("sig_123");

        when(paymentRepo.findById("order_123")).thenReturn(Optional.of(payment));
        when(transactionRepo.findByReferenceId("order_123")).thenReturn(Optional.of(transaction));
        when(walletQueryService.getBalance(1L)).thenReturn(
                WalletBalanceResponse.builder().balance(new BigDecimal("600.00")).build());

        try (MockedStatic<com.razorpay.Utils> utils = mockStatic(com.razorpay.Utils.class)) {
            utils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(), any())).thenReturn(true);

            razorpayService.verifyPayment(1L, request);
        }

        verify(walletCommandService).topup(1L, new BigDecimal("100.00"), "order_123");
        assertEquals("SUCCESS", payment.getStatus());
        assertEquals("pay_123", payment.getPaymentId());
        assertEquals(Transaction.TxnStatus.SUCCESS, transaction.getStatus());
        verify(paymentRepo).save(payment);
        verify(transactionRepo).save(transaction);
        verify(kafkaProducer).send(any(), any());
    }

    @Test
    void verifyPaymentFailureMarksPaymentAndTransactionFailed() throws Exception {
        ReflectionTestUtils.setField(razorpayService, "secret", "test-secret");

        Payment payment = Payment.builder()
                .orderId("order_123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .status("PENDING")
                .build();
        Transaction transaction = Transaction.builder()
                .receiverId(1L)
                .amount(new BigDecimal("100.00"))
                .status(Transaction.TxnStatus.PENDING)
                .type(Transaction.TxnType.TOPUP)
                .referenceId("order_123")
                .idempotencyKey("order_123")
                .build();
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("bad_sig");

        when(paymentRepo.findById("order_123")).thenReturn(Optional.of(payment));
        when(transactionRepo.findByReferenceId("order_123")).thenReturn(Optional.of(transaction));

        try (MockedStatic<com.razorpay.Utils> utils = mockStatic(com.razorpay.Utils.class)) {
            utils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(), any())).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> razorpayService.verifyPayment(1L, request));
            assertEquals("Invalid signature", ex.getMessage());
        }

        assertEquals("FAILED", payment.getStatus());
        assertEquals("pay_123", payment.getPaymentId());
        assertEquals(Transaction.TxnStatus.FAILED, transaction.getStatus());
        verify(walletCommandService, never()).topup(any(), any(), any());
        verify(paymentRepo).save(payment);
        verify(transactionRepo).save(transaction);
    }

    @Test
    void markPaymentFailedMarksPendingPaymentAndTransactionFailed() {
        Payment payment = Payment.builder()
                .orderId("order_123")
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .status("PENDING")
                .build();
        Transaction transaction = Transaction.builder()
                .receiverId(1L)
                .amount(new BigDecimal("100.00"))
                .status(Transaction.TxnStatus.PENDING)
                .type(Transaction.TxnType.TOPUP)
                .referenceId("order_123")
                .idempotencyKey("order_123")
                .build();
        PaymentFailureRequest request = new PaymentFailureRequest();
        request.setRazorpayOrderId("order_123");

        when(paymentRepo.findById("order_123")).thenReturn(Optional.of(payment));
        when(transactionRepo.findByReferenceId("order_123")).thenReturn(Optional.of(transaction));

        razorpayService.markPaymentFailed(1L, request);

        assertEquals("FAILED", payment.getStatus());
        assertEquals(Transaction.TxnStatus.FAILED, transaction.getStatus());
        verify(paymentRepo).save(payment);
        verify(transactionRepo).save(transaction);
        verify(walletCommandService, never()).topup(any(), any(), any());
    }
}
