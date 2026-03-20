package com.loyaltyService.admin_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "admin_username", nullable = false, length = 150) private String adminUsername;
    @Column(name = "action", nullable = false, length = 100) private String action;
    @Column(name = "target_id", length = 100) private String targetId;
    @Column(name = "details", length = 1000) private String details;
    @CreationTimestamp @Column(name = "timestamp", updatable = false) private LocalDateTime timestamp;
}
