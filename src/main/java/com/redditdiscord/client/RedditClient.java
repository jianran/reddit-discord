package com.redditdiscord.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redditdiscord.model.RedditChannel;
import com.redditdiscord.model.RedditPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RedditClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String userAgent;

    public RedditClient(@Value("${reddit.base-url}") String baseUrl,
                        @Value("${reddit.user-agent}") String userAgent,
                        ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    /**
     * Search for China/America related subreddits and return top channels by subscriber count.
     */
    public List<RedditChannel> searchChannels(List<String> keywords, int limit) {
        Set<RedditChannel> allChannels = new LinkedHashSet<>();

        for (String keyword : keywords) {
            try {
                String response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/subreddits/search.json")
                                .queryParam("q", keyword)
                                .queryParam("limit", 25)
                                .queryParam("sort", "relevance")
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                List<RedditChannel> channels = parseChannelResponse(response);
                allChannels.addAll(channels);
                log.debug("Found {} channels for keyword '{}'", channels.size(), keyword);

                // Rate limiting - Reddit allows ~60 req/min without auth
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Failed to search channels for keyword '{}': {}", keyword, e.getMessage());
            }
        }

        return allChannels.stream()
                .sorted(Comparator.comparingLong(RedditChannel::getSubscribers).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Search for China/America economic posts and return top posters by engagement.
     */
    public List<RedditPost> searchPosts(List<String> keywords, List<String> subreddits, int totalLimit) {
        Set<RedditPost> allPosts = new LinkedHashSet<>();

        // Search by keywords across all of Reddit
        for (String keyword : keywords) {
            try {
                String fullQuery = isChinese(keyword)
                        ? keyword + " 经济 贸易 政策"
                        : keyword + " economic trade policy";
                String response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search.json")
                                .queryParam("q", fullQuery)
                                .queryParam("sort", "top")
                                .queryParam("t", "month")
                                .queryParam("limit", 25)
                                .queryParam("restrict_sr", false)
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                List<RedditPost> posts = parsePostResponse(response);
                allPosts.addAll(posts);
                log.debug("Found {} posts for keyword '{}'", posts.size(), keyword);

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Failed to search posts for keyword '{}': {}", keyword, e.getMessage());
            }
        }

        // Also search within specific subreddits
        for (String subreddit : subreddits) {
            try {
                String fullQuery = isChineseSub(subreddit)
                        ? "中国 OR 美国 OR 经济 OR 贸易 OR 关税"
                        : "china OR america OR tariff OR trade OR economic";
                String response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/r/" + subreddit + "/search.json")
                                .queryParam("q", fullQuery)
                                .queryParam("sort", "top")
                                .queryParam("t", "month")
                                .queryParam("limit", 15)
                                .queryParam("restrict_sr", true)
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                List<RedditPost> posts = parsePostResponse(response);
                allPosts.addAll(posts);
                log.debug("Found {} posts in r/{}", posts.size(), subreddit);

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Failed to search posts in r/{}: {}", subreddit, e.getMessage());
            }
        }

        // Group by author and pick top posters by combined score
        Map<String, List<RedditPost>> postsByAuthor = allPosts.stream()
                .filter(p -> p.getAuthor() != null && !"[deleted]".equals(p.getAuthor()))
                .collect(Collectors.groupingBy(RedditPost::getAuthor));

        return postsByAuthor.values().stream()
                .map(authorPosts -> authorPosts.stream()
                        .max(Comparator.comparingInt(RedditPost::getScore))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(RedditPost::getScore).reversed())
                .limit(totalLimit)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<RedditChannel> parseChannelResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode children = root.path("data").path("children");
            List<RedditChannel> channels = new ArrayList<>();
            for (JsonNode child : children) {
                JsonNode data = child.path("data");
                RedditChannel channel = objectMapper.treeToValue(data, RedditChannel.class);
                channels.add(channel);
            }
            return channels;
        } catch (Exception e) {
            log.warn("Failed to parse channel response: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<RedditPost> parsePostResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode children = root.path("data").path("children");
            List<RedditPost> posts = new ArrayList<>();
            for (JsonNode child : children) {
                JsonNode data = child.path("data");
                // Skip non-t3 kinds (ads, etc.)
                if (!"t3".equals(child.path("kind").asText())) {
                    continue;
                }
                RedditPost post = objectMapper.treeToValue(data, RedditPost.class);
                posts.add(post);
            }
            return posts;
        } catch (Exception e) {
            log.warn("Failed to parse post response: {}", e.getMessage());
            return List.of();
        }
    }

    /** Check if a keyword contains Chinese characters. */
    private boolean isChinese(String text) {
        return text.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    /** Chinese-language focused subreddits where posts are primarily in Chinese. */
    private boolean isChineseSub(String subreddit) {
        return Set.of("China_irl", "Youmo", "saraba1st").contains(subreddit);
    }
}
