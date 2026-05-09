package com.redditdiscord.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class DiscordClient {

    private final WebClient webClient;
    private final String botToken;

    private static final String DISCORD_API_BASE = "https://discord.com/api/v10";

    public DiscordClient(@Value("${discord.bot-token}") String botToken) {
        this.botToken = botToken;
        this.webClient = WebClient.builder()
                .baseUrl(DISCORD_API_BASE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Create or get an existing DM channel with a user.
     */
    public String createDmChannel(String userId) {
        try {
            Map<String, Object> response = webClient.post()
                    .uri("/users/@me/channels")
                    .bodyValue(Map.of("recipient_id", userId))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                String channelId = (String) response.get("id");
                log.info("DM channel created/retrieved: {}", channelId);
                return channelId;
            }
        } catch (Exception e) {
            log.error("Failed to create DM channel for user {}: {}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * Send a message to a Discord channel (including DM channels).
     * Discord messages have a 2000-character limit.
     */
    public void sendMessage(String channelId, String content) {
        // Discord has a 2000 character limit per message
        if (content.length() > 2000) {
            // Split into chunks
            for (int i = 0; i < content.length(); i += 1900) {
                String chunk = content.substring(i, Math.min(i + 1900, content.length()));
                sendChunkedMessage(channelId, chunk);
            }
        } else {
            sendChunkedMessage(channelId, content);
        }
    }

    private void sendChunkedMessage(String channelId, String content) {
        try {
            Map<String, Object> body = Map.of("content", content);
            webClient.post()
                    .uri("/channels/" + channelId + "/messages")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            log.debug("Message sent to channel {}", channelId);
        } catch (Exception e) {
            log.error("Failed to send message to channel {}: {}", channelId, e.getMessage());
            throw new RuntimeException("Discord message send failed", e);
        }
    }
}
