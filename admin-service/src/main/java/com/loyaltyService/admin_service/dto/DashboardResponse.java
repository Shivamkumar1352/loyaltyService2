package com.loyaltyService.admin_service.dto;
import lombok.*;
import java.util.List;
import java.util.Map;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardResponse {
    private KycSummary kycSummary;
    private CampaignSummary campaignSummary;
    private List<RecentAction> recentActions;
    private Map<String, Long> metrics;
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KycSummary {
        private long pendingCount;
        private long approvedToday;
        private long rejectedToday;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CampaignSummary {
        private long activeCampaigns;
        private long totalCampaigns;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecentAction {
        private String admin;
        private String action;
        private String details;
        private String timestamp;
    }
}
