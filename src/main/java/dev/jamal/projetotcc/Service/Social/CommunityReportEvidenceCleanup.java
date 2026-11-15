package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.Repository.CommunityReportRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CommunityReportEvidenceCleanup {
    private final CommunityReportRepository reports;

    public CommunityReportEvidenceCleanup(CommunityReportRepository reports) {
        this.reports = reports;
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void removerEvidenciasExpiradas() {
        reports.deleteExpiredEvidence();
    }
}
