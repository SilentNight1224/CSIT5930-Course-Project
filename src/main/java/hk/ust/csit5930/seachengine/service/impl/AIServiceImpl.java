package hk.ust.csit5930.seachengine.service.impl;

import hk.ust.csit5930.seachengine.config.Config;
import hk.ust.csit5930.seachengine.service.AIService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class AIServiceImpl implements AIService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Resource
    private OpenAiChatModel model;

    @Override
    public String getSummary(String url) {
        return model.call(Config.AI_SUMMARY_PROMPT + "\n" + getPageContent(url));
    }

    String getPageContent(String url) {
        log.info("getPageContent: {}", url);
        return restTemplate.getForObject(url, String.class);
    }
}
