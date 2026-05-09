package com.redditdiscord.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EconomicScore {

    private String topic;

    @Builder.Default
    private int chinaScore = 50;

    @Builder.Default
    private int americaScore = 50;

    private String chinaTrending;   // BULLISH, BEARISH, NEUTRAL
    private String americaTrending; // BULLISH, BEARISH, NEUTRAL
    private String summary;
    private String keyFactors;
}
