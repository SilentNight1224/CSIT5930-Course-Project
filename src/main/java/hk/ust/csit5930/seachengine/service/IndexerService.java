package hk.ust.csit5930.seachengine.service;

import hk.ust.csit5930.seachengine.entity.DocumentRecord;
import org.jsoup.nodes.Document;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface IndexerService {

    void loadFromDB();

    void saveToDB();

    void indexPage(Document document, DocumentRecord record);

    Map<Integer, Integer> getHeadRelatedDocs(String word);

    Map<Integer, Integer> getBodyRelatedDocs(String word);

    Set<Integer> getRelatedDocs(Collection<String> words);

    double getIDF(String word);

    int getNumsOfPages();

    void setNumsOfPages(int numsOfPages);
}
