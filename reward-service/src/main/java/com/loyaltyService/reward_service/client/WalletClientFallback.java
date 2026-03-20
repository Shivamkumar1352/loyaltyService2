package com.loyaltyService.reward_service.client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
@Slf4j @Component
public class WalletClientFallback implements WalletClient {
    @Override
    public void credit(Long userId, BigDecimal amount) {
        log.warn("Wallet service unavailable — cashback not credited userId={}, amount={}", userId, amount);
    }
}
