package web;

import indexer.Indexer;
import search.SearchEngine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
public class WebServer {
    private SearchEngine searchEngine;

    public WebServer() throws IOException {
        // Step 1: Initialize Indexer and load the index file
        Indexer indexer = new Indexer("src/main/resources/stopwords.txt");
        indexer.loadIndex();

        // Step 2: Initialize SearchEngine
        this.searchEngine = new SearchEngine(indexer);
    }

    @GetMapping("/api/search")
    public List<Map<String, Object>> search(@RequestParam String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<Map.Entry<String, Double>> searchResults = searchEngine.search(query);

        System.out.println("Search results for query: " + query);
        for (Map.Entry<String, Double> entry : searchResults) {
            String pageId = entry.getKey();
            double score = entry.getValue();
            System.out.println("Page ID: " + pageId + ", Score: " + score);
        }

        for (Map.Entry<String, Double> entry : searchResults) {
            String pageId = entry.getKey();
            double score = entry.getValue();
            String title = searchEngine.getTitle(pageId);
            long lastModified = searchEngine.getLastModified(pageId);
            long size = searchEngine.getPageSize(pageId);
            Map<String, Integer> keywordFreq = searchEngine.getKeywordFrequency(pageId);
            List<String> parentLinks = searchEngine.getParentLinks(pageId);
            List<String> childLinks = searchEngine.getChildLinks(pageId);

            Map<String, Object> result = new HashMap<>();
            result.put("score", score);
            result.put("title", title);
            result.put("url", pageId);
            result.put("lastModified", new java.util.Date(lastModified));
            result.put("size", size);
            result.put("keywords", keywordFreq);
            result.put("parentLinks", parentLinks);
            result.put("childLinks", childLinks);
            results.add(result);
        }

        return results;
    }

    public static void main(String[] args) {
        SpringApplication.run(WebServer.class, args);
    }
}
