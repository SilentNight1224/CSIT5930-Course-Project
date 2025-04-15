package hk.ust.csit5930.seachengine.service;

import hk.ust.csit5930.seachengine.entity.SearchResult;
import hk.ust.csit5930.seachengine.entity.SuggestResult;

import java.util.List;

public interface SearchService {
    List<SearchResult> search(String query);

    List<SuggestResult> suggest(String query);
}
