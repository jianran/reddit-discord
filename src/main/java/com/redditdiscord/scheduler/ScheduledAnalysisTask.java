package com.redditdiscord.scheduler;

import com.redditdiscord.service.AnalysisOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledAnalysisTask {

    private final AnalysisOrchestrator orchestrator;

    /**
     * Run analysis on schedule defined in application.yml (default: 9 AM and 9 PM daily).
     */
    @Scheduled(cron = "${app.analysis.cron:0 0 9,21 * * *}")
    public void runScheduledAnalysis() {
        log.info("Scheduled analysis triggered");
        try {
            orchestrator.runAnalysis();
        } catch (Exception e) {
            log.error("Scheduled analysis failed", e);
        }
    }
}
