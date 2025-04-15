package hk.ust.csit5930.seachengine.service.impl;

import hk.ust.csit5930.seachengine.config.Config;
import hk.ust.csit5930.seachengine.service.StopStemService;
import hk.ust.csit5930.seachengine.utils.Porter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StopStemServiceImpl implements StopStemService {
    private final Porter porter;
    private final HashSet<String> stopWords;

    public StopStemServiceImpl() {
        porter = new Porter();
        stopWords = new HashSet<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(Config.STOP_WORDS_FILE)) {
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null) {
                    stopWords.add(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isStopWord(String str) {
        return stopWords.contains(str);
    }

    @Override
    public String stem(String str) {
        return porter.stripAffixes(str);
    }

    @Override
    public boolean tokenValidatedFilter(String str) {
        return !str.isBlank();
    }

    @Override
    public boolean stopWordFilter(String str) {
        return !stopWords.contains(str.toLowerCase());
    }

    @Override
    public String stemMapper(String str) {
        if (str.contains("\\W+")) {
            return Arrays.stream(str.split("\\W+"))
                    .map(this::stem)
                    .collect(Collectors.joining(" "));
        } else {
            return stem(str);
        }
    }
}
