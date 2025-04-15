package hk.ust.csit5930.seachengine.service.impl;

import hk.ust.csit5930.seachengine.config.Config;
import hk.ust.csit5930.seachengine.db.BodyInvertedIndex;
import hk.ust.csit5930.seachengine.db.HeadInvertedIndex;
import hk.ust.csit5930.seachengine.db.URLID;
import hk.ust.csit5930.seachengine.db.base.AbstractDBMap;
import hk.ust.csit5930.seachengine.entity.DocumentRecord;
import hk.ust.csit5930.seachengine.service.IndexerService;
import hk.ust.csit5930.seachengine.service.StopStemService;
import hk.ust.csit5930.seachengine.utils.WordsUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class IndexerServiceImpl implements IndexerService {

    private final StopStemService stopStemService;
    private final HeadInvertedIndex headInvertedIndex;
    private final BodyInvertedIndex bodyInvertedIndex;
    private HashMap<String, TreeMap<Integer, Integer>> headMap;
    private HashMap<String, TreeMap<Integer, Integer>> bodyMap;
    private int numsOfPages;

    public IndexerServiceImpl(
            StopStemService stopStemService,
            HeadInvertedIndex headInvertedIndex,
            BodyInvertedIndex bodyInvertedIndex,
            URLID urlID
    ) {
        this.stopStemService = stopStemService;
        this.bodyInvertedIndex = bodyInvertedIndex;
        this.headInvertedIndex = headInvertedIndex;
        loadFromDB();
        Integer pages = urlID.get(Config.CRAWLER_PAGE_COUNT_KEY);
        numsOfPages = pages == null ? 0 : pages;
    }

    @Override
    public void loadFromDB() {
        headMap = new HashMap<>();
        bodyMap = new HashMap<>();
        try {
            AbstractDBMap<String, TreeMap<Integer, Integer>>.Iterator it = bodyInvertedIndex.getIterator();
            while (it.isValid()) {
                bodyMap.put(it.key(), it.value());
                it.next();
            }
            it = headInvertedIndex.getIterator();
            while (it.isValid()) {
                headMap.put(it.key(), it.value());
                it.next();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveToDB() {
        for (Map.Entry<String, TreeMap<Integer, Integer>> entry : headMap.entrySet()) {
            headInvertedIndex.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, TreeMap<Integer, Integer>> entry : bodyMap.entrySet()) {
            bodyInvertedIndex.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void indexPage(Document document, DocumentRecord record) {
        TreeMap<String, Integer> headFreq = new TreeMap<>();
        TreeMap<String, Integer> bodyFreq = new TreeMap<>();
        String title = document.title();
        List<String> headTokens = WordsUtils.extractWords(title).stream()
                .filter(stopStemService::tokenValidatedFilter)
                .filter(stopStemService::stopWordFilter)
                .map(stopStemService::stemMapper)
                .toList();
        for (String token : headTokens) {
            headFreq.merge(token, 1, Integer::sum);
        }
        List<String> bodyTokens = WordsUtils.extractWords(document).stream()
                .filter(stopStemService::tokenValidatedFilter)
                .filter(stopStemService::stopWordFilter)
                .map(stopStemService::stemMapper)
                .toList();
        for (String token : bodyTokens) {
            bodyFreq.merge(token, 1, Integer::sum);
        }
        record.setTitle(title);
        record.setHeadFreq(headFreq);
        record.setBodyFreq(bodyFreq);
        record.setHeadWordCount(headTokens.size());
        record.setBodyWordCount(bodyTokens.size());
        mergeToMap(record);
    }

    void mergeToMap(DocumentRecord documentRecord) {
        Integer docId = documentRecord.getDocId();
        for (Map.Entry<String, Integer> entry : documentRecord.getHeadFreq().entrySet()) {
            TreeMap<Integer, Integer> tf = headMap.getOrDefault(entry.getKey(), new TreeMap<>());
            tf.put(docId, entry.getValue());
            headMap.put(entry.getKey(), tf);
        }
        for (Map.Entry<String, Integer> entry : documentRecord.getBodyFreq().entrySet()) {
            TreeMap<Integer, Integer> tf = bodyMap.getOrDefault(entry.getKey(), new TreeMap<>());
            tf.put(docId, entry.getValue());
            bodyMap.put(entry.getKey(), tf);
        }
    }

    @Override
    public Map<Integer, Integer> getHeadRelatedDocs(String word) {
        Map<Integer, Integer> headDocs = headInvertedIndex.get(word);
        return headDocs == null ? Collections.emptyMap() : headDocs;
    }

    @Override
    public Map<Integer, Integer> getBodyRelatedDocs(String word) {
        Map<Integer, Integer> bodyDocs =  bodyInvertedIndex.get(word);
        return bodyDocs == null ? Collections.emptyMap() : bodyDocs;
    }

    @Override
    public Set<Integer> getRelatedDocs(Collection<String> words) {
        HashSet<Integer> relatedDocs = new HashSet<>();
        for (String word : words) {
            relatedDocs.addAll(getHeadRelatedDocs(word).keySet());
            relatedDocs.addAll(getBodyRelatedDocs(word).keySet());
        }
        return relatedDocs;
    }

    @Override
    public double getIDF(String word) {
        Map<Integer, Integer> headDocs = getHeadRelatedDocs(word);
        Map<Integer, Integer> bodyDocs = getBodyRelatedDocs(word);
        if (headDocs.isEmpty() && bodyDocs.isEmpty()) {
            return 0.0;
        }
        int docFreq = headDocs.size() + bodyDocs.size();
        return Math.log(numsOfPages / (docFreq + 1.0));
    }

    @Override
    public int getNumsOfPages() {
        return numsOfPages;
    }

    @Override
    public void setNumsOfPages(int numsOfPages) {
        this.numsOfPages = numsOfPages;
    }
}
