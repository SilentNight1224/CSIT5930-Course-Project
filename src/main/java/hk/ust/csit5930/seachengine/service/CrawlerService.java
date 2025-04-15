package hk.ust.csit5930.seachengine.service;

import hk.ust.csit5930.seachengine.entity.DocumentRecord;

public interface CrawlerService {
    void init(String rootURL, int maxPages);

    void crawl();

    boolean isPageUpdated(DocumentRecord record);
}
