package com.loyaltyService.user_service.exception;

public class WalletServiceUnavailableException extends RuntimeException {
    public WalletServiceUnavailableException(String message) {
        super(message);
    }

    public WalletServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
