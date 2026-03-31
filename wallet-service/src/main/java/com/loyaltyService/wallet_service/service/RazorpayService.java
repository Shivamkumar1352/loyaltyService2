package com.loyaltyService.wallet_service.service;

import java.math.BigDecimal;

import com.loyaltyService.wallet_service.dto.PaymentVerifyRequest;
import com.razorpay.Order;
import com.razorpay.RazorpayException;

public interface RazorpayService {
	Order createOrder(Long userId, BigDecimal amount) throws RazorpayException;
	void verifyPayment(Long userId, PaymentVerifyRequest req) throws RazorpayException;
}
