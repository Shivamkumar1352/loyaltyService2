package com.loyaltyService.admin_service.repository;
import com.loyaltyService.admin_service.entity.AdminAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
@Repository
public interface AdminAuditRepository extends JpaRepository<AdminAudit, Long> {
    @Query("SELECT a FROM AdminAudit a WHERE (:admin IS NULL OR a.adminUsername = :admin) AND (:action IS NULL OR a.action = :action) AND a.timestamp BETWEEN :from AND :to ORDER BY a.timestamp DESC")
    Page<AdminAudit> search(@Param("admin") String admin, @Param("action") String action,
        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
    @Query("SELECT COUNT(a) FROM AdminAudit a WHERE a.timestamp >= :since")
    long countRecentActions(@Param("since") LocalDateTime since);
}
