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
public class RedditChannel {

    @JsonProperty("display_name")
    private String displayName;

    private String title;

    @JsonProperty("public_description")
    private String publicDescription;

    private long subscribers;

    @JsonProperty("created_utc")
    private long createdUtc;

    @JsonProperty("over18")
    private boolean over18;

    private String url;
}
