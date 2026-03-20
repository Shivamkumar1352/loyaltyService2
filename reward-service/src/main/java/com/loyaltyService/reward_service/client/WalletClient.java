package com.loyaltyService.reward_service.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
@FeignClient(name = "wallet-service", fallback = WalletClientFallback.class)
public interface WalletClient {
    @PostMapping("/wallet/internal/credit")
    void credit(@RequestParam("userId") Long userId, @RequestParam("amount") BigDecimal amount);
}
