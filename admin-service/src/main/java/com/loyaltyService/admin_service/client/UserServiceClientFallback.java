package com.loyaltyService.admin_service.client;

import com.loyaltyService.admin_service.dto.KycSummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public Page<KycSummaryDto> getPendingKyc(int page, int size, String role, String email) {
        log.warn("user-service unavailable — returning empty KYC list");
        return new PageImpl<>(Collections.emptyList());
    }

    @Override
    public KycSummaryDto approveKyc(Long kycId, String role, String email) {
        log.error("user-service unavailable — KYC {} not approved", kycId);
        return null;
    }

    @Override
    public KycSummaryDto approveKycByUserId(Long userId, String role, String email) {
        log.error("user-service unavailable — KYC for userId {} not approved", userId);
        return null;
    }

    @Override
    public KycSummaryDto rejectKyc(Long kycId, String reason, String role, String email) {
        log.error("user-service unavailable — KYC {} not rejected", kycId);
        return null;
    }

    @Override
    public KycSummaryDto rejectKycByUserId(Long userId, String reason, String role, String email) {
        log.error("user-service unavailable — KYC for userId {} not rejected", userId);
        return null;
    }
}
