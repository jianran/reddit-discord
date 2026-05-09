package com.redditdiscord.controller;

import com.redditdiscord.model.AnalysisResult;
import com.redditdiscord.service.AnalysisOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisOrchestrator orchestrator;

    @PostMapping("/run")
    public ResponseEntity<AnalysisResult> runAnalysis() {
        AnalysisResult result = orchestrator.runAnalysis();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/run-and-dm")
    public ResponseEntity<Map<String, Object>> runAnalysisAndDm() {
        AnalysisResult result = orchestrator.runAnalysis();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Analysis complete and DM sent",
                "resultId", result.getId(),
                "chinaScore", result.getEconomicScore().getChinaScore(),
                "americaScore", result.getEconomicScore().getAmericaScore(),
                "chinaTrending", result.getEconomicScore().getChinaTrending(),
                "americaTrending", result.getEconomicScore().getAmericaTrending()
        ));
    }
}
