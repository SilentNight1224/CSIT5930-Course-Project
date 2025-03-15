package indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Indexer {
    // 数据结构
    private Map<String, List<String>> bodyInvertedIndex; // 页面正文的倒排索引
    private Map<String, List<String>> titleInvertedIndex; // 页面标题的倒排索引
    private Set<String> stopWords; // 停用词列表
    private Map<String, String> documentTitles; // 页面标题
    private Map<String, Long> lastModifiedTimes; // 页面最后修改时间
    private Map<String, Long> pageSizes; // 页面长度
    private Map<String, Map<String, Integer>> keywordFrequencies; // 页面关键词频率
    private Map<String, List<String>> parentLinks; // 父链接
    private Map<String, List<String>> childLinks; // 子链接

    public Indexer(String stopWordsFilePath) throws IOException {
        this.bodyInvertedIndex = new HashMap<>();
        this.titleInvertedIndex = new HashMap<>();
        this.stopWords = new HashSet<>(Files.readAllLines(Paths.get(stopWordsFilePath)));
        this.documentTitles = new HashMap<>();
        this.lastModifiedTimes = new HashMap<>();
        this.pageSizes = new HashMap<>();
        this.keywordFrequencies = new HashMap<>();
        this.parentLinks = new HashMap<>();
        this.childLinks = new HashMap<>();
    }

    public void indexPage(String pageId, String html, long lastModified) {
        Document doc = Jsoup.parse(html);
        String text = doc.body().text();
        String title = doc.title(); // 提取页面标题

        // 保存页面标题
        documentTitles.put(pageId, title);

        // 保存页面最后修改时间
        lastModifiedTimes.put(pageId, lastModified);

        // 保存页面内容
        pageSizes.put(pageId, (long) html.length());

        // 初始化关键词频率
        keywordFrequencies.put(pageId, new HashMap<>());

        // 使用 StandardAnalyzer 进行分词，并结合 SnowballStemmer 进行词干提取
        Analyzer analyzer = new StandardAnalyzer();

        try (TokenStream tokenStream = new SnowballFilter(new StandardAnalyzer().tokenStream(null, text), "English")) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                String term = termAttribute.toString().toLowerCase();
                if (!stopWords.contains(term)) {
                    bodyInvertedIndex.computeIfAbsent(term, k -> new ArrayList<>()).add(pageId);
                    keywordFrequencies.get(pageId).merge(term, 1, Integer::sum);
                }
            }
            tokenStream.end();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 索引标题中的词干
        try (TokenStream tokenStream = new SnowballFilter(new StandardAnalyzer().tokenStream(null, text), "English")) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                String term = termAttribute.toString().toLowerCase();
                if (!stopWords.contains(term)) {
                    titleInvertedIndex.computeIfAbsent(term, k -> new ArrayList<>()).add(pageId);
                    keywordFrequencies.get(pageId).merge(term, 1, Integer::sum);
                }
            }
            tokenStream.end();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 保存所有数据到本地文件
    public void saveIndex() throws IOException {
        saveInvertedIndex(bodyInvertedIndex, "index_body.txt");
        saveInvertedIndex(titleInvertedIndex, "index_title.txt");
        saveMap(documentTitles, "titles.txt");
        saveMap(lastModifiedTimes, "lastModified.txt");
        saveMap(pageSizes, "sizes.txt");
        saveKeywordFrequencies(keywordFrequencies, "keywordFrequencies.txt");
    }

    // 加载所有数据从本地文件
    public void loadIndex() throws IOException {
        loadInvertedIndex("index_body.txt", bodyInvertedIndex);
        loadInvertedIndex("index_title.txt", titleInvertedIndex);
        loadInvertedIndex("child_parent_map.txt", parentLinks);
        loadInvertedIndex("parent_child_map.txt", childLinks);
        loadMap("titles.txt", documentTitles, String.class);
        loadMap("lastModified.txt", lastModifiedTimes, Long.class);
        loadMap("sizes.txt", pageSizes, Long.class);
        loadKeywordFrequencies("keywordFrequencies.txt", keywordFrequencies);
    }

    // 保存倒排索引到文件
    private void saveInvertedIndex(Map<String, List<String>> invertedIndex, String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : invertedIndex.entrySet()) {
            lines.add(entry.getKey() + "\t" + String.join(",", entry.getValue()));
        }
        Files.write(Path.of(filePath), lines);
    }

    // 加载倒排索引从文件
    private void loadInvertedIndex(String filePath, Map<String, List<String>> invertedIndex) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(filePath));
        for (String line : lines) {
            String[] parts = line.split("\t");
            String keyword = parts[0];
            String[] pageIds = parts[1].split(",");
            invertedIndex.put(keyword, Arrays.asList(pageIds));
        }
    }

    // 保存普通 Map 到文件
    private <K, V> void saveMap(Map<K, V> map, String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            lines.add(entry.getKey() + "\t" + entry.getValue());
        }
        Files.write(Path.of(filePath), lines);
    }

    // 加载普通 Map 从文件
    private <K, V> void loadMap(String filePath, Map<K, V> map, Class<V> valueClass) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(filePath));
        for (String line : lines) {
            String[] parts = line.split("\t");
            if (parts.length >= 2) {
                K key = (K) parts[0];
                V value;
                try {
                    value = valueClass.getConstructor(String.class).newInstance(parts[1]);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to convert value: " + parts[1] + " to type " + valueClass.getName(), e);
                }
                map.put(key, value);
            } else {
                System.err.println("Invalid line format in file " + filePath + ": " + line);
            }
        }
    }

    // 保存关键词频率到文件
    private void saveKeywordFrequencies(Map<String, Map<String, Integer>> keywordFrequencies, String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : keywordFrequencies.entrySet()) {
            String pageId = entry.getKey();
            Map<String, Integer> frequencies = entry.getValue();
            StringBuilder sb = new StringBuilder(pageId);
            for (Map.Entry<String, Integer> freqEntry : frequencies.entrySet()) {
                sb.append("\t\t").append(freqEntry.getKey()).append("\t").append(freqEntry.getValue());
            }
            lines.add(sb.toString());
        }
        Files.write(Path.of(filePath), lines);
    }

    // 加载关键词频率从文件
    private void loadKeywordFrequencies(String filePath, Map<String, Map<String, Integer>> keywordFrequencies) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(filePath));
        for (String line : lines) {
            String[] parts = line.split("\t\t");
            String pageId = parts[0];
            Map<String, Integer> frequencies = new HashMap<>();
            for (int i = 1; i < parts.length; i++) {
                String[] freqParts = parts[i].split("\t");
                frequencies.put(freqParts[0], Integer.parseInt(freqParts[1]));
            }
            keywordFrequencies.put(pageId, frequencies);
        }
    }

    // Getter 和 Setter 方法
    public Map<String, List<String>> getBodyInvertedIndex() {
        return bodyInvertedIndex;
    }

    public Map<String, List<String>> getTitleInvertedIndex() {
        return titleInvertedIndex;
    }

    public String getTitle(String pageId) {
        return documentTitles.getOrDefault(pageId, "No Title");
    }

    public long getLastModified(String pageId) {
        return lastModifiedTimes.getOrDefault(pageId, 0L);
    }

    public List<String> getParentLinks(String pageId) {
        return parentLinks.getOrDefault(pageId, Collections.emptyList());
    }

    public List<String> getChildLinks(String pageId) {
        return childLinks.getOrDefault(pageId, Collections.emptyList());
    }

    public Map<String, Integer> getKeywordFrequency(String pageId) {
        return keywordFrequencies.getOrDefault(pageId, new HashMap<>());
    }

    public long getPageSize(String pageId) {
        return pageSizes.getOrDefault(pageId, 0L);
    }
}