package com.redditdiscord.service;

import com.redditdiscord.client.RedditClient;
import com.redditdiscord.model.RedditChannel;
import com.redditdiscord.model.RedditPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedditService {

    private final RedditClient redditClient;

    @Value("${reddit.search-keywords}")
    private List<String> searchKeywords;

    @Value("${reddit.subreddits}")
    private List<String> subreddits;

    @Value("${app.top-channels-count}")
    private int topChannelsCount;

    @Value("${app.top-posters-count}")
    private int topPostersCount;

    public List<RedditChannel> findTopChannels() {
        log.info("Searching top {} China/America related channels...", topChannelsCount);
        List<RedditChannel> channels = redditClient.searchChannels(searchKeywords, topChannelsCount);
        log.info("Found {} top channels", channels.size());
        return channels;
    }

    public List<RedditPost> findTopPosters() {
        log.info("Searching top {} China/America economic posters...", topPostersCount);
        List<RedditPost> posters = redditClient.searchPosts(searchKeywords, subreddits, topPostersCount);
        log.info("Found {} top posters", posters.size());
        return posters;
    }
}
