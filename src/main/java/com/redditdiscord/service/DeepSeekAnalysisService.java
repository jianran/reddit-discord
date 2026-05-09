package com.redditdiscord.service;

import com.redditdiscord.model.AnalysisResult;
import com.redditdiscord.model.EconomicScore;
import com.redditdiscord.model.RedditChannel;
import com.redditdiscord.model.RedditPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekAnalysisService {

    private final ChatClient.Builder chatClientBuilder;

    private static final String SYSTEM_PROMPT = """
            You are an expert geopolitical and economic analyst. You analyze Reddit discussions
            about China and America to produce structured economic assessments.

            Your task:
            1. Summarize the relevance of each subreddit/channel to China-America economic topics
            2. Summarize each poster's key point in ONE concise sentence
            3. Analyze overall sentiment and produce an economic trending assessment

            IMPORTANT: Return your analysis in the EXACT JSON format specified. No markdown, no extra text.
            Scores must be integers 0-100. Trending must be one of: BULLISH, BEARISH, NEUTRAL.

            BULLISH = positive economic outlook (growth, opportunity, recovery)
            BEARISH = negative economic outlook (decline, risk, contraction)
            NEUTRAL = mixed or no clear direction
            """;

    public AnalysisResult analyze(List<RedditChannel> channels, List<RedditPost> posters) {
        log.info("Starting DeepSeek analysis for {} channels and {} posters", channels.size(), posters.size());

        String userPrompt = buildAnalysisPrompt(channels, posters);

        try {
            String response = chatClientBuilder.build()
                    .prompt(new Prompt(List.of(
                            new SystemMessage(SYSTEM_PROMPT),
                            new UserMessage(userPrompt)
                    )))
                    .call()
                    .content();

            log.info("DeepSeek analysis completed, parsing response...");
            return parseAnalysisResponse(response, channels, posters);

        } catch (Exception e) {
            log.error("DeepSeek analysis failed: {}", e.getMessage(), e);
            return buildFallbackAnalysis(channels, posters, e.getMessage());
        }
    }

    private String buildAnalysisPrompt(List<RedditChannel> channels, List<RedditPost> posters) {
        StringBuilder sb = new StringBuilder();

        sb.append("## Top China/America Related Subreddits\n\n");
        for (int i = 0; i < channels.size(); i++) {
            RedditChannel ch = channels.get(i);
            sb.append(String.format("%d. **r/%s** — %s (%,d subscribers)\n   Description: %s\n\n",
                    i + 1, ch.getDisplayName(), ch.getTitle(),
                    ch.getSubscribers(),
                    ch.getPublicDescription() != null ? ch.getPublicDescription().substring(0, Math.min(200, ch.getPublicDescription().length())) : "N/A"));
        }

        sb.append("## Top China/America Economic Posters\n\n");
        for (int i = 0; i < posters.size(); i++) {
            RedditPost post = posters.get(i);
            String content = post.getSelfText() != null
                    ? post.getSelfText().substring(0, Math.min(300, post.getSelfText().length()))
                    : "[Link post]";
            sb.append(String.format("""
                            %d. **u/%s** in r/%s (Score: %d, Comments: %d)
                               Title: %s
                               Content: %s
                               URL: %s

                            """,
                    i + 1, post.getAuthor(), post.getSubreddit(),
                    post.getScore(), post.getNumComments(),
                    post.getTitle(), content, post.getFullUrl()));
        }

        sb.append("""
                ---
                Based on the above data, produce a JSON analysis with this EXACT structure:
                {
                  "channelSummaries": [
                    {"name": "r/subreddit", "relevance": "one sentence describing China/America relevance"}
                  ],
                  "posterSummaries": [
                    {"author": "username", "postTitle": "title", "subreddit": "r/...", "score": 100, "comments": 50, "aiSummary": "one sentence summary of their viewpoint", "postUrl": "url"}
                  ],
                  "economicScore": {
                    "topic": "China-America Economic Relations",
                    "chinaScore": 65,
                    "americaScore": 55,
                    "chinaTrending": "BEARISH",
                    "americaTrending": "NEUTRAL",
                    "summary": "A 2-3 sentence overall assessment of the economic discourse",
                    "keyFactors": "Comma-separated list of key factors driving sentiment"
                  }
                }

                Analyze sentiment carefully:
                - chinaScore/americaScore: 0=extremely bearish, 50=neutral, 100=extremely bullish
                - Base scores on the tone, upvotes, comment counts, and content of discussions
                - Consider whether discussions reflect positively or negatively on each country's economy
                """);

        return sb.toString();
    }

    private AnalysisResult parseAnalysisResponse(String jsonResponse, List<RedditChannel> channels, List<RedditPost> posters) {
        try {
            // Clean markdown code fences if present
            String cleaned = jsonResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            DeepSeekResponse dsResponse = mapper.readValue(cleaned, DeepSeekResponse.class);

            return buildResult(dsResponse, channels, posters);
        } catch (Exception e) {
            log.warn("Failed to parse DeepSeek JSON response, using fallback: {}", e.getMessage());
            return buildFallbackAnalysis(channels, posters, "JSON parse error: " + e.getMessage());
        }
    }

    private AnalysisResult buildResult(DeepSeekResponse dsResponse, List<RedditChannel> channels, List<RedditPost> posters) {
        List<AnalysisResult.RedditChannelSummary> channelSummaries = new ArrayList<>();
        if (dsResponse.channelSummaries != null) {
            channelSummaries = dsResponse.channelSummaries.stream()
                    .map(s -> AnalysisResult.RedditChannelSummary.builder()
                            .name(s.name)
                            .title(channels.stream()
                                    .filter(c -> c.getDisplayName().equals(s.name.replace("r/", "")))
                                    .findFirst()
                                    .map(RedditChannel::getTitle)
                                    .orElse(""))
                            .subscribers(channels.stream()
                                    .filter(c -> c.getDisplayName().equals(s.name.replace("r/", "")))
                                    .findFirst()
                                    .map(RedditChannel::getSubscribers)
                                    .orElse(0L))
                            .relevance(s.relevance)
                            .build())
                    .collect(Collectors.toList());
        }

        List<AnalysisResult.PosterSummary> posterSummaries = new ArrayList<>();
        if (dsResponse.posterSummaries != null) {
            posterSummaries = dsResponse.posterSummaries.stream()
                    .map(s -> AnalysisResult.PosterSummary.builder()
                            .author(s.author)
                            .postTitle(s.postTitle)
                            .subreddit(s.subreddit)
                            .score(s.score)
                            .comments(s.comments)
                            .aiSummary(s.aiSummary)
                            .postUrl(s.postUrl)
                            .build())
                    .collect(Collectors.toList());
        }

        EconomicScore economicScore = EconomicScore.builder()
                .topic("China-America Economic Relations")
                .chinaScore(dsResponse.economicScore != null ? dsResponse.economicScore.chinaScore : 50)
                .americaScore(dsResponse.economicScore != null ? dsResponse.economicScore.americaScore : 50)
                .chinaTrending(dsResponse.economicScore != null ? dsResponse.economicScore.chinaTrending : "NEUTRAL")
                .americaTrending(dsResponse.economicScore != null ? dsResponse.economicScore.americaTrending : "NEUTRAL")
                .summary(dsResponse.economicScore != null ? dsResponse.economicScore.summary : "Analysis unavailable")
                .keyFactors(dsResponse.economicScore != null ? dsResponse.economicScore.keyFactors : "")
                .build();

        String fullSummary = dsResponse.economicScore != null
                ? dsResponse.economicScore.summary + " Key factors: " + dsResponse.economicScore.keyFactors
                : "No summary available";

        return AnalysisResult.builder()
                .id(UUID.randomUUID().toString().substring(0, 8))
                .generatedAt(Instant.now())
                .topChannels(channelSummaries)
                .topPosters(posterSummaries)
                .economicScore(economicScore)
                .fullSummary(fullSummary)
                .build();
    }

    private AnalysisResult buildFallbackAnalysis(List<RedditChannel> channels, List<RedditPost> posters, String error) {
        List<AnalysisResult.RedditChannelSummary> channelSummaries = channels.stream()
                .map(ch -> AnalysisResult.RedditChannelSummary.builder()
                        .name("r/" + ch.getDisplayName())
                        .title(ch.getTitle())
                        .subscribers(ch.getSubscribers())
                        .relevance("China/America related discussion community")
                        .build())
                .collect(Collectors.toList());

        List<AnalysisResult.PosterSummary> posterSummaries = posters.stream()
                .map(p -> AnalysisResult.PosterSummary.builder()
                        .author(p.getAuthor())
                        .postTitle(p.getTitle())
                        .subreddit("r/" + p.getSubreddit())
                        .score(p.getScore())
                        .comments(p.getNumComments())
                        .aiSummary("Discussion about China-America economic topics")
                        .postUrl(p.getFullUrl())
                        .build())
                .collect(Collectors.toList());

        return AnalysisResult.builder()
                .id(UUID.randomUUID().toString().substring(0, 8))
                .generatedAt(Instant.now())
                .topChannels(channelSummaries)
                .topPosters(posterSummaries)
                .economicScore(EconomicScore.builder()
                        .topic("China-America Economic Relations")
                        .chinaScore(50)
                        .americaScore(50)
                        .chinaTrending("NEUTRAL")
                        .americaTrending("NEUTRAL")
                        .summary("AI analysis failed: " + error + ". Showing raw data without AI summary.")
                        .keyFactors("AI analysis unavailable")
                        .build())
                .fullSummary("AI analysis failed: " + error + ". Data collected but not analyzed.")
                .build();
    }

    // --- Inner classes for JSON deserialization ---

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeepSeekResponse {
        public List<ChannelSummary> channelSummaries;
        public List<PosterSummary> posterSummaries;
        public EconomicScoreData economicScore;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChannelSummary {
        public String name;
        public String relevance;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class PosterSummary {
        public String author;
        public String postTitle;
        public String subreddit;
        public int score;
        public int comments;
        public String aiSummary;
        public String postUrl;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class EconomicScoreData {
        public String topic;
        public int chinaScore;
        public int americaScore;
        public String chinaTrending;
        public String americaTrending;
        public String summary;
        public String keyFactors;
    }
}
