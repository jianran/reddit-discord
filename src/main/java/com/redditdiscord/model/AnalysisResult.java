package com.redditdiscord.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    private String id;
    private Instant generatedAt;

    private List<RedditChannelSummary> topChannels;
    private List<PosterSummary> topPosters;

    private EconomicScore economicScore;
    private String fullSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedditChannelSummary {
        private String name;
        private String title;
        private long subscribers;
        private String relevance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PosterSummary {
        private String author;
        private String postTitle;
        private String subreddit;
        private int score;
        private int comments;
        private String aiSummary;
        private String postUrl;
    }
}
