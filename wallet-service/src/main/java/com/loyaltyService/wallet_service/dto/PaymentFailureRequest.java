package com.loyaltyService.wallet_service.dto;

import lombok.Data;

@Data
public class PaymentFailureRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
}
