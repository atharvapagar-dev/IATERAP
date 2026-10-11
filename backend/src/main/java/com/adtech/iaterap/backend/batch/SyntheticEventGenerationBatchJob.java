package com.adtech.iaterap.backend.batch;

import com.adtech.iaterap.backend.dto.SyntheticEventGenerationResponse;
import com.adtech.iaterap.backend.repository.AircraftRepository;
import com.adtech.iaterap.backend.service.SyntheticEventGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled batch job skeleton for synthetic event generation (AI-247 / T14-B3).
 *
 * Runs nightly (02:00 local) so it never fires unintentionally in a plain
 * development or production boot unless the scheduler is active.
 *
 * SYNTHETIC DATA NOTICE: all events produced are clearly labelled synthetic
 * fleet evidence per Sec 3.2 of the IATERAP proposal. This job never accepts
 * or processes live aircraft telemetry.
 */
@Component
public class SyntheticEventGenerationBatchJob {

    private static final Logger log = LoggerFactory.getLogger(SyntheticEventGenerationBatchJob.class);

    private final AircraftRepository aircraftRepository;
    private final SyntheticEventGenerationService eventGenerationService;

    public SyntheticEventGenerationBatchJob(AircraftRepository aircraftRepository,
                                            SyntheticEventGenerationService eventGenerationService) {
        this.aircraftRepository = aircraftRepository;
        this.eventGenerationService = eventGenerationService;
    }

    /**
     * Nightly run at 02:00 - generates synthetic event evidence for every
     * known aircraft from its persisted validation outcomes.
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void generateNightly() {
        log.info("[SYNTHETIC-BATCH] Starting nightly synthetic event generation run");
        int processed = 0;
        int failed = 0;

        for (var aircraft : aircraftRepository.findAll()) {
            try {
                SyntheticEventGenerationResponse response =
                        eventGenerationService.generate(aircraft.getId());
                log.info("[SYNTHETIC-BATCH] Aircraft {}: {} runs considered, {} instances, {} events created, {} already present",
                        aircraft.getId(),
                        response.validationRunsConsidered(),
                        response.syntheticInstancesCreated(),
                        response.eventsCreated(),
                        response.eventsAlreadyPresent());
                processed++;
            } catch (Exception ex) {
                log.error("[SYNTHETIC-BATCH] Failed for aircraft {}: {}", aircraft.getId(), ex.getMessage(), ex);
                failed++;
            }
        }

        log.info("[SYNTHETIC-BATCH] Nightly run complete - {} aircraft processed, {} failed", processed, failed);
    }
}