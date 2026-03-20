package com.loyaltyService.admin_service.service;
import com.loyaltyService.admin_service.dto.CampaignRequest;
import com.loyaltyService.admin_service.entity.AdminAudit;
import com.loyaltyService.admin_service.entity.Campaign;
import com.loyaltyService.admin_service.repository.AdminAuditRepository;
import com.loyaltyService.admin_service.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Slf4j @Service @RequiredArgsConstructor
public class CampaignService {
    private final CampaignRepository campaignRepo;
    private final AdminAuditRepository auditRepo;

    @Transactional
    public Campaign create(CampaignRequest req, String adminEmail) {
        Campaign c = Campaign.builder()
            .name(req.getName()).description(req.getDescription()).type(req.getType())
            .bonusPoints(req.getBonusPoints()).multiplier(req.getMultiplier())
            .minTransactionAmount(req.getMinTransactionAmount())
            .startDate(req.getStartDate()).endDate(req.getEndDate())
            .createdBy(adminEmail).active(true).build();
        Campaign saved = campaignRepo.save(c);
        auditRepo.save(AdminAudit.builder().adminUsername(adminEmail)
            .action("CAMPAIGN_CREATED").targetId(saved.getId().toString())
            .details("Campaign: " + saved.getName()).build());
        log.info("Campaign created: id={}, name={}, by={}", saved.getId(), saved.getName(), adminEmail);
        return saved;
    }

    public List<Campaign> getAll() { return campaignRepo.findAll(); }
    public List<Campaign> getActive() { return campaignRepo.findByActiveTrue(); }

    @Transactional
    public Campaign deactivate(Long id, String adminEmail) {
        Campaign c = campaignRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
        c.setActive(false);
        auditRepo.save(AdminAudit.builder().adminUsername(adminEmail)
            .action("CAMPAIGN_DEACTIVATED").targetId(id.toString()).build());
        return campaignRepo.save(c);
    }
}
