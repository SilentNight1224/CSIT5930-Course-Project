package hk.ust.csit5930.seachengine.service.impl;

import hk.ust.csit5930.seachengine.config.Config;
import hk.ust.csit5930.seachengine.db.base.AbstractDBMap;
import hk.ust.csit5930.seachengine.db.ForwardIndex;
import hk.ust.csit5930.seachengine.db.URLID;
import hk.ust.csit5930.seachengine.entity.DocumentRecord;
import hk.ust.csit5930.seachengine.service.CrawlerService;
import hk.ust.csit5930.seachengine.service.IndexerService;
import hk.ust.csit5930.seachengine.utils.SslUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.*;

@Service
@Slf4j
public class CrawlerServiceImpl implements CrawlerService {
    private final ForwardIndex forwardIndex;
    private final URLID urlID;
    private final IndexerService indexerService;
    private String rootURL;
    private int maxPages;
    Set<String> visited;
    Set<String> todos;
    Queue<String> queue;
    HashMap<Integer, DocumentRecord> documentRecords;

    public CrawlerServiceImpl(ForwardIndex forwardIndex, URLID urlID, IndexerService indexerService) {
        this.forwardIndex = forwardIndex;
        this.urlID = urlID;
        this.indexerService = indexerService;
    }

    void loadFromDB() {
        documentRecords = new HashMap<>();
        try {
            AbstractDBMap<Integer, DocumentRecord>.Iterator it = forwardIndex.getIterator();
            while (it.isValid()) {
                DocumentRecord record = it.value();
                documentRecords.put(record.getDocId(), record);
                it.next();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void saveToDB() {
        for (DocumentRecord record : documentRecords.values()) {
            forwardIndex.put(record.getDocId(), record);
        }
    }

    Integer getDocId(String url) {
        Integer docId = urlID.get(url);
        if (docId == null) {
            docId = urlID.getAndIncrementNextID();
            urlID.put(url, docId);
        }
        return docId;
    }

    DocumentRecord getDocumentRecord(String url) {
        Integer docId = getDocId(url);
        DocumentRecord record = documentRecords.get(docId);
        if (record == null) {
            record = DocumentRecord.builder()
                    .docId(docId)
                    .url(url)
                    .build();
            documentRecords.put(docId, record);
        }
        return record;
    }

    @Override
    public void init(String rootURL, int maxPages) {
        this.rootURL = rootURL;
        this.maxPages = maxPages;
    }

    @Override
    public void crawl() {
        try {
            SslUtils.ignoreSsl();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (rootURL == null) {
            log.warn("rootURL is null");
            return;
        }
        loadFromDB();
        visited = new HashSet<>();
        todos = new HashSet<>();
        queue = new LinkedList<>();
        queue.add(rootURL);
        int counter = 0;
        while (!queue.isEmpty() && counter < maxPages) {
            String currentURL = queue.poll();
            if (currentURL == null || visited.contains(currentURL)) {
                continue;
            }
            DocumentRecord record = getDocumentRecord(currentURL);
            if (!isPageUpdated(record)) {
                continue;
            }
            try {
                Connection connection = Jsoup.connect(currentURL).followRedirects(false);
                Connection.Response response = connection.execute();
                Document doc = response.parse();
                log.info("\nFetched: {}\nTitle: {}\nLinks found:", currentURL, doc.title());
                URL url = new URL(currentURL);
                String domain = url.getHost();
                String baseUri = url.getProtocol() + "://" + url.getHost() + url.getPath();
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String absUrl = link.attr("abs:href");
                    if (!absUrl.startsWith("http")) {
                        absUrl = baseUri + absUrl;
                    }
                    if (!visited.contains(absUrl) && !todos.contains(absUrl)) {
                        try {
                            URL linkUrlObj = new URL(absUrl);
                            if (linkUrlObj.getHost().equals(domain)) {
                                queue.add(absUrl);
                                todos.add(absUrl);
                                DocumentRecord child = getDocumentRecord(absUrl);
                                record.addChild(child.getDocId());
                                child.addParent(record.getDocId());
                                log.info(absUrl);
                            }
                        } catch (MalformedURLException e) {
                            log.error("Invalid URL: {}", absUrl);
                        }
                    }
                }
                String lastModified = response.header("Last-Modified");
                if (lastModified == null || lastModified.isEmpty()) {
                    lastModified = response.header("Date");
                }
                record.setLastModified(Date.parse(lastModified));
                indexerService.indexPage(doc, record);
                record.setSize((long) doc.body().text().length());
                counter++;
                visited.add(currentURL);
                todos.remove(currentURL);
            } catch (HttpStatusException | SocketTimeoutException e) {
                queue.add(currentURL);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        saveToDB();
        indexerService.saveToDB();
        indexerService.setNumsOfPages(documentRecords.size());
        urlID.put(Config.CRAWLER_PAGE_COUNT_KEY, documentRecords.size());
        visited = null;
        todos = null;
        queue = null;
    }

    @Override
    public boolean isPageUpdated(DocumentRecord record) {
        if (record == null || record.getLastModified() == null) {
            return true;
        }
        try {
            URL u = new URL(record.getUrl());
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("HEAD");
            long lastModified = conn.getLastModified();
            return lastModified > record.getLastModified();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
