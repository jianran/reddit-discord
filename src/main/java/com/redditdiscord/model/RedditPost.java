package com.redditdiscord.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditPost {

    private String id;
    private String title;

    @JsonProperty("selftext")
    private String selfText;

    private String author;
    private String subreddit;

    @JsonProperty("created_utc")
    private long createdUtc;

    private int score;

    @JsonProperty("num_comments")
    private int numComments;

    private String url;

    @JsonProperty("permalink")
    private String permalink;

    private boolean over18;

    public String getFullUrl() {
        return "https://www.reddit.com" + permalink;
    }
}
