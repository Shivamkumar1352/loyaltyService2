package com.loyaltyService.admin_service.controller;

import com.loyaltyService.admin_service.client.UserServiceClient;
import com.loyaltyService.admin_service.dto.ApiResponse;
import com.loyaltyService.admin_service.dto.KycSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/kyc")
@RequiredArgsConstructor
@Tag(name = "Admin KYC", description = "KYC review — approve and reject submissions")
@SecurityRequirement(name = "bearerAuth")
public class KycController {

    private final UserServiceClient userServiceClient;

    @GetMapping("/pending")
    @Operation(summary = "List all pending KYC submissions")
    public ResponseEntity<ApiResponse<Page<KycSummaryDto>>> pending(
            @RequestHeader("X-User-Role")  String role,
            @RequestHeader("X-User-Email") String email,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<KycSummaryDto> result = userServiceClient.getPendingKyc(page, size, role, email);
        return ResponseEntity.ok(ApiResponse.ok("Pending KYC fetched", result));
    }

    @PostMapping("/{kycId}/approve")
    @Operation(summary = "Approve KYC by KYC record ID")
    public ResponseEntity<ApiResponse<KycSummaryDto>> approveByKycId(
            @PathVariable Long kycId,
            @RequestHeader("X-User-Role")  String role,
            @RequestHeader("X-User-Email") String email) {

        KycSummaryDto result = userServiceClient.approveKyc(kycId, role, email);
        return ResponseEntity.ok(ApiResponse.ok("KYC approved", result));
    }

    @PostMapping("/user/{userId}/approve")
    @Operation(summary = "Approve KYC by User ID — approves the latest pending KYC for that user")
    public ResponseEntity<ApiResponse<KycSummaryDto>> approveByUserId(
            @PathVariable Long userId,
            @RequestHeader("X-User-Role")  String role,
            @RequestHeader("X-User-Email") String email) {

        KycSummaryDto result = userServiceClient.approveKycByUserId(userId, role, email);
        return ResponseEntity.ok(ApiResponse.ok("KYC approved for userId: " + userId, result));
    }

    @PostMapping("/{kycId}/reject")
    @Operation(summary = "Reject KYC by KYC record ID")
    public ResponseEntity<ApiResponse<KycSummaryDto>> rejectByKycId(
            @PathVariable Long kycId,
            @RequestParam String reason,
            @RequestHeader("X-User-Role")  String role,
            @RequestHeader("X-User-Email") String email) {

        KycSummaryDto result = userServiceClient.rejectKyc(kycId, reason, role, email);
        return ResponseEntity.ok(ApiResponse.ok("KYC rejected", result));
    }

    @PostMapping("/user/{userId}/reject")
    @Operation(summary = "Reject KYC by User ID")
    public ResponseEntity<ApiResponse<KycSummaryDto>> rejectByUserId(
            @PathVariable Long userId,
            @RequestParam String reason,
            @RequestHeader("X-User-Role")  String role,
            @RequestHeader("X-User-Email") String email) {

        KycSummaryDto result = userServiceClient.rejectKycByUserId(userId, reason, role, email);
        return ResponseEntity.ok(ApiResponse.ok("KYC rejected for userId: " + userId, result));
    }
}
