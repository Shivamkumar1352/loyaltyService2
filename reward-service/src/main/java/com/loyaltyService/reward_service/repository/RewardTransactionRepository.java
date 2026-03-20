package com.loyaltyService.reward_service.repository;

import com.loyaltyService.reward_service.entity.RewardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    List<RewardTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    @Query("SELECT t FROM RewardTransaction t WHERE t.userId = :userId AND t.expiryDate BETWEEN :now AND :soon")
    List<RewardTransaction> findExpiringPoints(@Param("userId") Long userId,
                                               @Param("now") LocalDateTime now, @Param("soon") LocalDateTime soon);
}
