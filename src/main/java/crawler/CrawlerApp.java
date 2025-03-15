package crawler;

import indexer.Indexer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import utils.SslUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class CrawlerApp {
    public static void main(String[] args) {
        try {
            // Step 1: Initialize Indexer
            Indexer indexer = new Indexer("src/main/resources/stopwords.txt");

            // Step 2: Run crawler to fetch pages
            SslUtils.ignoreSsl();
            Crawler crawler = new Crawler("https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm", 300, indexer);
            crawler.crawl();

            // Step 3: Index the fetched pages
            for (String url : crawler.getVisited()) {
                Document doc = Jsoup.connect(url).get();
                String html = doc.html();
                long lastModified = indexer.getLastModified(url);
                if (lastModified == 0 || isPageUpdated(url, lastModified)) {
                    indexer.indexPage(url, html, System.currentTimeMillis());
                }
            }

            // Step 4: Save the inverted index to a file
            indexer.saveIndex();
            System.out.println("Indexing completed.");

            // Optional: Save parent-child link structure to files
            saveParentChildMap(crawler.getParentChildMap(), "parent_child_map.txt");
            saveChildParentMap(crawler.getChildParentMap(), "child_parent_map.txt");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isPageUpdated(String url, long indexedLastModified) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");
        long currentLastModified = connection.getLastModified();
        return currentLastModified > indexedLastModified;
    }

    // Helper method to save parent-child map to a file
    private static void saveParentChildMap(Map<String, Set<String>> parentChildMap, String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : parentChildMap.entrySet()) {
            sb.append(entry.getKey()).append("\t").append(String.join(",", entry.getValue())).append("\n");
        }
        java.nio.file.Files.write(java.nio.file.Paths.get(filePath), sb.toString().getBytes());
        //System.out.println("Parent-Child Map saved to " + filePath);
    }

    // Helper method to save child-parent map to a file
    private static void saveChildParentMap(Map<String, String> childParentMap, String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : childParentMap.entrySet()) {
            sb.append(entry.getKey()).append("\t").append(entry.getValue()).append("\n");
        }
        java.nio.file.Files.write(java.nio.file.Paths.get(filePath), sb.toString().getBytes());
        //System.out.println("Child-Parent Map saved to " + filePath);
    }
}
