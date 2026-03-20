package com.loyaltyService.admin_service.service;
import com.loyaltyService.admin_service.dto.DashboardResponse;
import com.loyaltyService.admin_service.repository.AdminAuditRepository;
import com.loyaltyService.admin_service.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final CampaignRepository    campaignRepo;
    private final AdminAuditRepository  auditRepo;

    public DashboardResponse getDashboard() {
        LocalDateTime now        = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();

        List<DashboardResponse.RecentAction> recent = auditRepo
                .search(null, null, now.minusDays(1), now, PageRequest.of(0, 10))
                .stream()
                .map(a -> DashboardResponse.RecentAction.builder()
                        .admin(a.getAdminUsername())
                        .action(a.getAction())
                        .details(a.getDetails())
                        .timestamp(a.getTimestamp().toString())
                        .build())
                .collect(Collectors.toList());

        long actionsToday    = auditRepo.countRecentActions(startOfDay);
        long activeCampaigns = campaignRepo.countByActiveTrue();
        long totalCampaigns  = campaignRepo.count();

        return DashboardResponse.builder()
                .kycSummary(DashboardResponse.KycSummary.builder()
                        // KYC counts come from user-service via the KYC endpoints.
                        // Admin can call GET /api/admin/kyc/pending for live data.
                        .pendingCount(0)
                        .approvedToday(0)
                        .rejectedToday(0)
                        .build())
                .campaignSummary(DashboardResponse.CampaignSummary.builder()
                        .activeCampaigns(activeCampaigns)
                        .totalCampaigns(totalCampaigns)
                        .build())
                .recentActions(recent)
                .metrics(Map.of(
                        "actionsToday",    actionsToday,
                        "activeCampaigns", activeCampaigns,
                        "totalCampaigns",  totalCampaigns
                ))
                .build();
    }
}