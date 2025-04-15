package hk.ust.csit5930.seachengine.controller;

import hk.ust.csit5930.seachengine.config.Config;
import hk.ust.csit5930.seachengine.entity.InitResult;
import hk.ust.csit5930.seachengine.entity.SearchResult;
import hk.ust.csit5930.seachengine.service.CrawlerService;
import hk.ust.csit5930.seachengine.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class Search {

    private final CrawlerService crawlerService;
    private final SearchService searchService;

    public Search(CrawlerService crawlerService, SearchService searchService) {
        this.crawlerService = crawlerService;
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public List<SearchResult> search(@RequestParam(name = "q") String query) {
        log.info("search query: {}", query);
        return searchService.search(query);
    }

    @GetMapping("/suggest")
    public String suggest(@RequestParam(name = "q") String query) {
        log.info("suggest");
        return "suggest: " + query;
    }

    @GetMapping("/init")
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
