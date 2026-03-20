package com.loyaltyService.admin_service.client;

import com.loyaltyService.admin_service.dto.KycSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/admin/kyc/pending")
    Page<KycSummaryDto> getPendingKyc(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Email") String email);

    @PostMapping("/api/admin/kyc/{kycId}/approve")
    KycSummaryDto approveKyc(
            @PathVariable Long kycId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Email") String email);

    @PostMapping("/api/admin/kyc/user/{userId}/approve")
    KycSummaryDto approveKycByUserId(
            @PathVariable Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Email") String email);

    @PostMapping("/api/admin/kyc/{kycId}/reject")
    KycSummaryDto rejectKyc(
            @PathVariable Long kycId,
            @RequestParam String reason,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Email") String email);

    @PostMapping("/api/admin/kyc/user/{userId}/reject")
    KycSummaryDto rejectKycByUserId(
            @PathVariable Long userId,
            @RequestParam String reason,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Email") String email);
}

