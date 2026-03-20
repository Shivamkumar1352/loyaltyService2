package com.loyaltyService.reward_service.controller;

import com.loyaltyService.reward_service.dto.ApiResponse;
import com.loyaltyService.reward_service.dto.RedeemRequest;
import com.loyaltyService.reward_service.dto.RewardSummaryDto;
import com.loyaltyService.reward_service.entity.Redemption;
import com.loyaltyService.reward_service.entity.RewardItem;
import com.loyaltyService.reward_service.entity.RewardTransaction;
import com.loyaltyService.reward_service.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("api/rewards")
@RequiredArgsConstructor
@Tag(name = "Rewards", description = "Points, tiers, catalog, redemption")
@SecurityRequirement(name = "bearerAuth")
public class RewardController {
    private final RewardService rewardService;

    @GetMapping("/summary")
    @Operation(summary = "Get reward summary — points, tier, next tier")
    public ResponseEntity<ApiResponse<RewardSummaryDto>> summary(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Summary fetched", rewardService.getSummary(userId)));
    }

    @GetMapping("/catalog")
    @Operation(summary = "Browse reward catalog")
    public ResponseEntity<ApiResponse<List<RewardItem>>> catalog() {
        return ResponseEntity.ok(ApiResponse.ok("Catalog fetched", rewardService.getCatalog()));
    }

    @PostMapping("/redeem")
    @Operation(summary = "Redeem a catalog reward")
    public ResponseEntity<ApiResponse<Redemption>> redeem(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RedeemRequest req) {
        Redemption r = rewardService.redeemReward(userId, req.getRewardId());
        return ResponseEntity.ok(ApiResponse.ok("Redemption successful", r));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Reward transaction history")
    public ResponseEntity<ApiResponse<List<RewardTransaction>>> transactions(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Transactions fetched", rewardService.getTransactions(userId)));
    }

    @PostMapping("/internal/create-account")
    public ResponseEntity<Void> createAccount(@RequestParam Long userId) {
        rewardService.createAccountIfNotExists(userId);
        return ResponseEntity.ok().build();
    }


    /** Internal endpoint — called by wallet-service after topup */
    @PostMapping("/internal/earn")
    public ResponseEntity<Void> earnInternal(
            @RequestParam Long userId,
            @RequestParam java.math.BigDecimal amount) {
        rewardService.earnPoints(userId, amount);
        return ResponseEntity.ok().build();
    }
}
