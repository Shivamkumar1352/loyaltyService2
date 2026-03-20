package com.loyaltyService.admin_service.dto;
import com.loyaltyService.admin_service.entity.Campaign;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaignRequest {
    @NotBlank private String name;
    private String description;
    @NotNull private Campaign.CampaignType type;
    @Min(0) private Integer bonusPoints;
    @DecimalMin("1.0") private BigDecimal multiplier;
    @DecimalMin("0") private BigDecimal minTransactionAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
