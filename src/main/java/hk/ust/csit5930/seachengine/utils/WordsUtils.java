package hk.ust.csit5930.seachengine.utils;

import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class WordsUtils {

    public static List<String> extractWords(Document document) {
        return extractWords(document.body().text());
    }

    public static List<String> extractWords(String content) {
        return Arrays.stream(content.split("\\W+")).toList();
    }

}
