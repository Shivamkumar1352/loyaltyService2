package com.loyaltyService.admin_service.repository;
import com.loyaltyService.admin_service.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByActiveTrue();
    long countByActiveTrue();
}
