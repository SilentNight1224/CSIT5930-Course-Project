package hk.ust.csit5930.seachengine.controller;

import hk.ust.csit5930.seachengine.config.Config;
import hk.ust.csit5930.seachengine.entity.InitResult;
import hk.ust.csit5930.seachengine.entity.SearchResult;
import hk.ust.csit5930.seachengine.service.CrawlerService;
import hk.ust.csit5930.seachengine.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class Search {

    private final CrawlerService crawlerService;
    private final SearchService searchService;

    public Search(CrawlerService crawlerService, SearchService searchService) {
        this.crawlerService = crawlerService;
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ModelAndView search(@RequestParam(name = "q") String query) {
        log.info("search query: {}", query);
        SearchResult result = null;
        if (query.contains("(") && query.contains(")")) {
            String[] split = query.split("[\\[\\](),\\s]+");
            if (split.length % 2 == 0) {
                Map<String, Double> queryFreq = new HashMap<>();
                try {
                    for (int i = 0; i < split.length; i += 2) {
                        String word = split[i];
                        Double freq = Double.parseDouble(split[i + 1]);
                        queryFreq.merge(word, freq, Double::sum);
                    }
                    result = searchService.searchWithVector(queryFreq);
                } catch (NullPointerException | NumberFormatException e) {
                    log.error(e.getMessage());
                }
            }
        }
        if (result == null) {
            result = searchService.search(query);
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("originalQuery", query);
        modelAndView.addObject("processedQuery", result.getProcessedQuery());
        modelAndView.addObject("data", result.getResults());
        modelAndView.setViewName("results");
        return modelAndView;
    }

    @GetMapping("/suggest")
    @ResponseBody
    public String suggest(@RequestParam(name = "q") String query) {
        log.info("suggest");
        return "suggest: " + query;
    }

    @GetMapping("/init")
    @ResponseBody
    public InitResult init() {
        log.info("init");
        crawlerService.init(Config.CRAWLER_ROOT_URL, Config.CRAWLER_MAX_PAGES);
        try {
            crawlerService.crawl();
            return InitResult.success();
        } catch (Exception e) {
            return InitResult.error(e.getMessage());
        }
    }
}
