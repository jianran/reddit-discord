package com.redditdiscord.service;

import com.redditdiscord.model.AnalysisResult;
import com.redditdiscord.model.RedditChannel;
import com.redditdiscord.model.RedditPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisOrchestrator {

    private final RedditService redditService;
    private final DeepSeekAnalysisService deepSeekAnalysisService;
    private final DiscordNotificationService discordNotificationService;

    /**
     * Run the full pipeline: fetch Reddit data → analyze with DeepSeek → send Discord DM.
     */
    public AnalysisResult runAnalysis() {
        log.info("=== Starting China-America Economic Analysis Pipeline ===");

        // Step 1: Fetch Reddit data
        List<RedditChannel> channels = redditService.findTopChannels();
        List<RedditPost> posters = redditService.findTopPosters();

        if (channels.isEmpty() && posters.isEmpty()) {
            log.warn("No Reddit data found. Skipping analysis.");
            return AnalysisResult.builder()
                    .fullSummary("No Reddit data found. Check Reddit API availability.")
                    .build();
        }

        // Step 2: Analyze with DeepSeek
        AnalysisResult result = deepSeekAnalysisService.analyze(channels, posters);

        // Step 3: Send Discord DM
        discordNotificationService.sendAnalysisDm(result);

        log.info("=== Pipeline complete: {} channels, {} posters, China={}/{}, America={}/{} ===",
                result.getTopChannels().size(), result.getTopPosters().size(),
                result.getEconomicScore().getChinaScore(), result.getEconomicScore().getChinaTrending(),
                result.getEconomicScore().getAmericaScore(), result.getEconomicScore().getAmericaTrending());

        return result;
    }
}
