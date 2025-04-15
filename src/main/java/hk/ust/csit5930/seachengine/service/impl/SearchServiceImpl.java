package hk.ust.csit5930.seachengine.service.impl;

import hk.ust.csit5930.seachengine.db.ForwardIndex;
import hk.ust.csit5930.seachengine.entity.DocumentRecord;
import hk.ust.csit5930.seachengine.entity.SearchResult;
import hk.ust.csit5930.seachengine.entity.SuggestResult;
import hk.ust.csit5930.seachengine.service.IndexerService;
import hk.ust.csit5930.seachengine.service.SearchService;
import hk.ust.csit5930.seachengine.service.StopStemService;
import hk.ust.csit5930.seachengine.utils.WordsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final ForwardIndex forwardIndex;
    private final IndexerService indexerService;
    private final StopStemService stopStemService;

    public SearchServiceImpl(ForwardIndex forwardIndex, IndexerService indexerService, StopStemService stopStemService) {
        this.forwardIndex = forwardIndex;
        this.indexerService = indexerService;
        this.stopStemService = stopStemService;
    }

    @Override
    public List<SearchResult> search(String query) {
        log.info("Searching for {}", query);
        List<String> queryTokens = WordsUtils.extractWords(query).stream()
                .filter(stopStemService::tokenValidatedFilter)
                .filter(stopStemService::stopWordFilter)
                .map(stopStemService::stemMapper)
                .toList();
        if (queryTokens.isEmpty()) {
            queryTokens = WordsUtils.extractWords(query).stream()
                    .filter(stopStemService::tokenValidatedFilter)
                    .map(stopStemService::stemMapper)
                    .toList();
        }
        log.info("Found {} tokens: {}", queryTokens.size(), queryTokens);
        HashMap<String, Integer> queryFreq = new HashMap<>();
        for (String queryToken : queryTokens) {
            queryFreq.merge(queryToken, 1, Integer::sum);
        }
        Set<Integer> relatedDocs = indexerService.getRelatedDocs(queryFreq.keySet());
        Map<Integer, Double> similarities = getSimilarity(relatedDocs, queryFreq);
        return similarities.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(50)
                .map(entry -> convertToSearchResult(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public List<SuggestResult> suggest(String query) {
        return List.of();
    }

    public Map<Integer, Double> getSimilarity(Set<Integer> docs, Map<String, Integer> query) {
        Map<Integer, Map<String, Double>> docsVector = getDocVectors(docs);
        Map<String, Double> queryVector = getQueryVector(query);
        Map<Integer, Double> similarities = new HashMap<>();
        for (Integer doc: docs) {
            Map<String, Double> docVector = docsVector.get(doc);
            if (docVector != null) {
                similarities.put(doc, getCosineSimilarity(docVector, queryVector));
            }
        }
        return similarities;
    }

    public Map<Integer, Map<String, Double>> getDocVectors(Set<Integer> docs) {
        Set<String> terms = new HashSet<>();
        Map<Integer, Map<String, Double>> docVectors = new HashMap<>();
        for (Integer doc : docs) {
            Map<String, Double> termFreq = forwardIndex.getNormalizedTF(doc);
            terms.addAll(termFreq.keySet());
            docVectors.put(doc, termFreq);
        }
        Map<String, Double> idf = new HashMap<>();
        for (String term : terms) {
            idf.put(term, indexerService.getIDF(term));
        }
        docVectors.replaceAll((k, v) ->
            v.entrySet().stream()
                    .filter(e -> idf.containsKey(e.getKey()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> idf.get(e.getKey()) * e.getValue()))
        );
        return docVectors;
    }

    public Map<String, Double> getQueryVector(Map<String, Integer> queryFreq) {
        double maxFreq = queryFreq.values().stream().max(Integer::compare).orElse(1).doubleValue();
        return queryFreq.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() / maxFreq));
    }

    public double getCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        double len1 = Math.sqrt(vector1.values().stream().mapToDouble(d -> d * d).sum());
        double len2 = Math.sqrt(vector2.values().stream().mapToDouble(d -> d * d).sum());
        double product = vector1.entrySet().stream()
                .filter(entry -> vector2.containsKey(entry.getKey()))
                .mapToDouble(entry -> entry.getValue() * vector2.get(entry.getKey()))
                .sum();
        return product / (len1 * len2);
    }

    public SearchResult convertToSearchResult(Integer docId, Double score) {
        DocumentRecord record = forwardIndex.get(docId);
        List<String> keywords = forwardIndex.getNormalizedTF(docId).entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(5)
                .map(entry -> String.format("%s(%.2f)", entry.getKey(), entry.getValue()))
                .toList();
        Set<Integer> ids = record.getParents();
        List<String> parents = ids == null ? null : ids.stream()
                .limit(5)
                .map(id -> forwardIndex.get(id).getUrl())
                .toList();
        ids = record.getChildren();
        List<String> children = ids == null ? null : ids.stream()
                .limit(5)
                .map(id -> forwardIndex.get(id).getUrl())
                .toList();
        return SearchResult.builder()
                .docId(docId)
                .score(score)
                .title(record.getTitle())
                .url(record.getUrl())
                .lastModified(record.getLastModified())
                .size(record.getSize())
                .keywords(keywords)
                .parents(parents)
                .children(children)
                .build();
    }
}
