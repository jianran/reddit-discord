package com.redditdiscord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RedditDiscordApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedditDiscordApplication.class, args);
    }
}
