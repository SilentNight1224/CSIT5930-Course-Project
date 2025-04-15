package hk.ust.csit5930.seachengine.service;

public interface StopStemService {

    boolean isStopWord(String str);

    String stem(String str);

    boolean tokenValidatedFilter(String str);

    boolean stopWordFilter(String str);

    String stemMapper(String str);
}
