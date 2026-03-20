package com.loyaltyService.admin_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Campaign {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "name", nullable = false, length = 100) private String name;
    @Column(name = "description", length = 500) private String description;
    @Enumerated(EnumType.STRING) @Column(name = "type", nullable = false, length = 30) private CampaignType type;
    @Column(name = "bonus_points") private Integer bonusPoints;
    @Column(name = "multiplier", precision = 4, scale = 2) private BigDecimal multiplier;
    @Column(name = "min_transaction_amount", precision = 10, scale = 2) private BigDecimal minTransactionAmount;
    @Column(name = "active", nullable = false) @Builder.Default private Boolean active = true;
    @Column(name = "start_date") private LocalDateTime startDate;
    @Column(name = "end_date") private LocalDateTime endDate;
    @Column(name = "created_by", length = 100) private String createdBy;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
    public enum CampaignType { BONUS_POINTS, POINTS_MULTIPLIER, CASHBACK }
}
