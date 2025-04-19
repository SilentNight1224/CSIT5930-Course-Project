package hk.ust.csit5930.seachengine.controller;

import hk.ust.csit5930.seachengine.entity.SummaryResult;
import hk.ust.csit5930.seachengine.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class Summary {
    private final AIService aiService;

    public Summary(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/summary")
    public SummaryResult getSummary(@RequestParam(name = "url") String url) {
        log.info("Summary page {}", url);
        try {
            String summary = aiService.getSummary(url);
            return SummaryResult.success(summary);
        } catch (Exception e) {
            return SummaryResult.error(e.getMessage());
        }
    }
}
