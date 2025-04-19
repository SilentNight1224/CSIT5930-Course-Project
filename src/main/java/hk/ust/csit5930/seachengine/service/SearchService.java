package hk.ust.csit5930.seachengine.service;

import hk.ust.csit5930.seachengine.entity.SearchResult;
import hk.ust.csit5930.seachengine.entity.SuggestResult;

import java.util.List;
import java.util.Map;

public interface SearchService {
    SearchResult search(String query);

    SearchResult searchWithVector(Map<String, Double> queryWithFreq);

    List<SuggestResult> suggest(String query);
}
