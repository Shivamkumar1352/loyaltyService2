package com.loyaltyService.admin_service.controller;
import com.loyaltyService.admin_service.dto.ApiResponse;
import com.loyaltyService.admin_service.dto.CampaignRequest;
import com.loyaltyService.admin_service.dto.DashboardResponse;
import com.loyaltyService.admin_service.entity.Campaign;
import com.loyaltyService.admin_service.service.AdminDashboardService;
import com.loyaltyService.admin_service.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin dashboard, campaigns, audit logs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final AdminDashboardService dashboardService;
    private final CampaignService campaignService;

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard overview")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard loaded", dashboardService.getDashboard()));
    }

    @PostMapping("/campaigns")
    @Operation(summary = "Create a new campaign")
    public ResponseEntity<ApiResponse<Campaign>> createCampaign(
            @Valid @RequestBody CampaignRequest req,
            @RequestHeader("X-User-Email") String adminEmail) {
        Campaign c = campaignService.create(req, adminEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Campaign created", c));
    }

    @GetMapping("/campaigns")
    @Operation(summary = "List all campaigns")
    public ResponseEntity<ApiResponse<List<Campaign>>> campaigns() {
        return ResponseEntity.ok(ApiResponse.ok("Campaigns fetched", campaignService.getAll()));
    }

    @PostMapping("/campaigns/{id}/deactivate")
    @Operation(summary = "Deactivate a campaign")
    public ResponseEntity<ApiResponse<Campaign>> deactivate(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String adminEmail) {
        return ResponseEntity.ok(ApiResponse.ok("Campaign deactivated", campaignService.deactivate(id, adminEmail)));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() { return ResponseEntity.ok("Admin Service is running"); }
}
