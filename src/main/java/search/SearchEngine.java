package search;

import indexer.Indexer;

import java.util.*;

public class SearchEngine {
    private Indexer indexer;

    public SearchEngine(Indexer indexer) {
        this.indexer = indexer;
    }

    public List<Map.Entry<String, Double>> search(String query) {
        System.out.println("Processing search query: " + query);

        // 分析查询，支持短语匹配
        Map<String, Integer> queryTermFreq = new HashMap<>();
        String[] queryTerms = query.split("\\W+");
        for (String term : queryTerms) {
            queryTermFreq.put(term.toLowerCase(), queryTermFreq.getOrDefault(term.toLowerCase(), 0) + 1);
        }

        /*System.out.println("Query terms frequency:");
        for (Map.Entry<String, Integer> entry : queryTermFreq.entrySet()) {
            System.out.println("Term: " + entry.getKey() + ", Frequency: " + entry.getValue());
        }*/

        // 获取正文和标题的倒排索引
        Map<String, List<String>> bodyInvertedIndex = indexer.getBodyInvertedIndex();
        Map<String, List<String>> titleInvertedIndex = indexer.getTitleInvertedIndex();

        /*System.out.println("Body Inverted Index:");
        bodyInvertedIndex.forEach((term, pageIds) -> {
            System.out.println("Term: " + term + ", Page IDs: " + pageIds);
        });*/

        /*System.out.println("Title Inverted Index:");
        titleInvertedIndex.forEach((term, pageIds) -> {
            System.out.println("Term: " + term + ", Page IDs: " + pageIds);
        });*/

        // 收集文档中每个词项的频率
        Map<String, Map<String, Integer>> documentTermFreq = new HashMap<>();

        // 查询正文索引
        for (String term : queryTermFreq.keySet()) {
            List<String> pageIds = bodyInvertedIndex.get(term);
            if (pageIds != null) {
                for (String pageId : pageIds) {
                    documentTermFreq.computeIfAbsent(pageId, k -> new HashMap<>())
                            .merge(term, 1, Integer::sum);
                }
            }
        }

        // 查询标题索引
        for (String term : queryTermFreq.keySet()) {
            List<String> pageIds = titleInvertedIndex.get(term);
            if (pageIds != null) {
                for (String pageId : pageIds) {
                    documentTermFreq.computeIfAbsent(pageId, k -> new HashMap<>())
                            .merge(term, 1, Integer::sum);
                }
            }
        }

        /*System.out.println("Document Term Frequency:");
        documentTermFreq.forEach((pageId, termFreq) -> {
            System.out.println("Page ID: " + pageId + ", Term Frequencies: " + termFreq);
        });*/

        // 计算文档的 TF-IDF 权重
        Map<String, Double> docScores = new HashMap<>();
        double maxQueryTF = queryTermFreq.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        for (Map.Entry<String, Map<String, Integer>> entry : documentTermFreq.entrySet()) {
            String pageId = entry.getKey();
            Map<String, Integer> termFreq = entry.getValue();
            double maxDocTF = termFreq.values().stream().mapToInt(Integer::intValue).max().orElse(1);

            double queryNorm = 0.0;
            double docNorm = 0.0;
            double dotProduct = 0.0;

            for (String term : queryTermFreq.keySet()) {
                if (termFreq.containsKey(term)) {
                    double tf = termFreq.get(term);
                    double idf = Math.log((bodyInvertedIndex.size() + titleInvertedIndex.size()) /
                            (double) (bodyInvertedIndex.getOrDefault(term, Collections.emptyList()).size() +
                                    titleInvertedIndex.getOrDefault(term, Collections.emptyList()).size()));
                    double tfidf = (tf / maxDocTF) * idf;
                    double queryWeight = (queryTermFreq.get(term) / maxQueryTF) * idf;

                    queryNorm += queryWeight * queryWeight;
                    docNorm += tfidf * tfidf;
                    dotProduct += queryWeight * tfidf;
                    System.out.println("Page ID: " + pageId + ", Term: " + term + ", TF-IDF: " + tfidf);
                }
            }

            // 计算余弦相似度
            double cosineSimilarity = dotProduct / (Math.sqrt(queryNorm) * Math.sqrt(docNorm));

            // 检查标题匹配，提升权重
            if (isTermInTitle(pageId, queryTerms)) {
                cosineSimilarity *= 2; // 假设标题匹配的权重提升为原来的两倍
            }

            docScores.put(pageId, cosineSimilarity);
        }

        /*System.out.println("Cosine Similarity Calculation:");
        for (Map.Entry<String, Double> entry : docScores.entrySet()) {
            System.out.println("Page ID: " + entry.getKey() + ", Cosine Similarity: " + entry.getValue());
        }*/

        // 排序并返回前 50 个文档
        List<Map.Entry<String, Double>> rankedDocs = new ArrayList<>(docScores.entrySet());
        rankedDocs.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return rankedDocs.subList(0, Math.min(50, rankedDocs.size()));
    }

    // 新增方法：获取页面标题
    public String getTitle(String pageId) {
        return indexer.getTitle(pageId);
    }

    // 新增方法：获取页面最后修改时间
    public long getLastModified(String pageId) {
        return indexer.getLastModified(pageId);
    }

    // 新增方法：获取页面内容
    public long getPageSize(String pageId) {
        return indexer.getPageSize(pageId);
    }

    // 新增方法：获取关键词频率
    public Map<String, Integer> getKeywordFrequency(String pageId) {
        return indexer.getKeywordFrequency(pageId);
    }

    // 新增方法：获取父链接
    public List<String> getParentLinks(String pageId) {
        return indexer.getParentLinks(pageId);
    }

    // 新增方法：获取子链接
    public List<String> getChildLinks(String pageId) {
        return indexer.getChildLinks(pageId);
    }

    private boolean isTermInTitle(String pageId, String[] queryTerms) {
        String title = indexer.getTitle(pageId);
        for (String term : queryTerms) {
            if (title.toLowerCase().contains(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
