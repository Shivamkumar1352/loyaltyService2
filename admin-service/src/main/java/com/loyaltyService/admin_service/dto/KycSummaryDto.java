package com.loyaltyService.admin_service.dto;

import lombok.*;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycSummaryDto {
    private Long kycId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String docType;
    private String docNumber;
    private String status;
    private String rejectionReason;
    private Instant submittedAt;
    private Instant updatedAt;
}
