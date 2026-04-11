package com.loyaltyService.wallet_service.client;

import com.loyaltyService.wallet_service.dto.ApiResponse;
import com.loyaltyService.wallet_service.dto.UserLookupResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public ApiResponse<UserLookupResponse> findUserForTransfer(String email, String phone) {
        log.warn("User service unavailable for transfer lookup. email={}, phone={}", email, phone);
        return ApiResponse.<UserLookupResponse>builder()
                .success(false)
                .message("User service unavailable")
                .build();
    }
}
