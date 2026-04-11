package com.loyaltyService.wallet_service.client;

import com.loyaltyService.wallet_service.dto.ApiResponse;
import com.loyaltyService.wallet_service.dto.UserLookupResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/users/internal/lookup")
    ApiResponse<UserLookupResponse> findUserForTransfer(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone
    );
}
