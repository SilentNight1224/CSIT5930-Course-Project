package crawler;

import indexer.Indexer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Crawler {
    private String startUrl;
    private int maxPages;
    private Set<String> visited; // 已访问的页面
    private Queue<String> queue; // 待抓取的页面队列
    private Map<String, Set<String>> parentChildMap; // 父页面和子页面的关系
    private Map<String, String> childParentMap; // 子页面和父页面的关系
    private Indexer indexer; // 索引器，用于检查页面是否需要抓取

    public Crawler(String startUrl, int maxPages, Indexer indexer) {
        this.startUrl = startUrl;
        this.maxPages = maxPages;
        this.visited = new HashSet<>();
        this.queue = new LinkedList<>();
        this.parentChildMap = new HashMap<>();
        this.childParentMap = new HashMap<>();
        this.indexer = indexer;
        this.queue.add(startUrl);
    }

    public void crawl() {
        while (!queue.isEmpty() && visited.size() < maxPages) {
            String currentUrl = queue.poll();
            if (visited.contains(currentUrl)) {
                continue;
            }

            if (!shouldFetch(currentUrl)) {
                continue;
            }

            try {
                Document doc = Jsoup.connect(currentUrl).get();
                System.out.println("Fetched: " + currentUrl);
                System.out.println("Page Title: " + doc.title());

                Elements links = doc.select("a[href]");
                URL startUrlObj = new URL(startUrl);
                String domain = startUrlObj.getHost(); // 获取域名
                String baseUri = startUrlObj.getProtocol() + "://" + startUrlObj.getHost() + startUrlObj.getPath();

                System.out.println("Links found on " + currentUrl + ":");
                for (Element link : links) {
                    String absUrl = link.attr("abs:href");
                    if (!absUrl.startsWith("http")) {
                        absUrl = baseUri + absUrl; // 将相对路径转换为绝对路径
                    }
                    System.out.println("  " + absUrl);

                    if (!visited.contains(absUrl)) {
                        try {
                            URL linkUrlObj = new URL(absUrl);
                            if (linkUrlObj.getHost().equals(domain)) {
                                queue.add(absUrl);
                                parentChildMap.computeIfAbsent(currentUrl, k -> new HashSet<>()).add(absUrl);
                                childParentMap.put(absUrl, currentUrl);
                            }
                        } catch (MalformedURLException e) {
                            System.err.println("Invalid URL: " + absUrl);
                        }
                    }
                }

                visited.add(currentUrl);
            } catch (IOException e) {
                System.err.println("Error fetching " + currentUrl + ": " + e.getMessage());
            }
        }
        System.out.println("Total pages fetched: " + visited.size());
    }

    private boolean shouldFetch(String url) {
        // 检查页面是否需要抓取
        if (!visited.contains(url)) {
            // 页面未抓取过，需要抓取
            return true;
        }

        // 页面已抓取过，检查最后修改时间
        try {
            URL u = new URL(url);
            long lastModified = u.openConnection().getLastModified();
            long indexedLastModified = indexer.getLastModified(url);

            if (lastModified > indexedLastModified) {
                // 页面有更新，需要重新抓取
                return true;
            }
        } catch (IOException e) {
            System.err.println("Error checking last modified date for " + url + ": " + e.getMessage());
        }

        // 页面未更新，无需抓取
        return false;
    }

    public Set<String> getVisited() {
        return visited;
    }

    public Map<String, Set<String>> getParentChildMap() {
        return parentChildMap;
    }

    public Map<String, String> getChildParentMap() {
        return childParentMap;
    }
}
