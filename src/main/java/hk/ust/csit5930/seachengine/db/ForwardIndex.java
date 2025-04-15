package hk.ust.csit5930.seachengine.db;

import hk.ust.csit5930.seachengine.config.Config;
import hk.ust.csit5930.seachengine.db.base.AbstractDBMap;
import hk.ust.csit5930.seachengine.entity.DocumentRecord;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ForwardIndex extends AbstractDBMap<Integer, DocumentRecord> {

    public ForwardIndex() throws RocksDBException {
        super("forward_index");
    }

    @Override
    public byte[] keyToBytes(Integer key) {
        byte[] src = new byte[4];
        src[3] = (byte) ((key >> 24) & 0xFF);
        src[2] = (byte) ((key >> 16) & 0xFF);
        src[1] = (byte) ((key >> 8) & 0xFF);
        src[0] = (byte) (key & 0xFF);
        return src;
    }

    @Override
    public Integer bytesToKey(byte[] key) {
        return ( (key[0] & 0xFF)
                | ((key[1] & 0xFF)<<8)
                | ((key[2] & 0xFF)<<16)
                | ((key[3] & 0xFF)<<24));
    }

    public Map<String, Double> getNormalizedTF(Integer docId) {
        DocumentRecord record = get(docId);
        if (record == null) {
            return new HashMap<>();
        }
        Map<String, Integer> headFreq = record.getHeadFreq();
        Map<String, Integer> bodyFreq = record.getBodyFreq();
        if (headFreq == null) headFreq = new HashMap<>();
        if (bodyFreq == null) bodyFreq = new HashMap<>();
        headFreq.replaceAll((k, v) -> v * Config.INDEX_HEAD_TOKEN_WEIGHT);
        Map<String, Double> normalizedTF = Stream.of(headFreq, bodyFreq)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Double.valueOf(entry.getValue()),
                        Double::sum
                ));
        double maxFreq = normalizedTF.values().stream().max(Double::compare).orElse(1.0);
        normalizedTF.replaceAll((k, v) -> v / maxFreq);
        return normalizedTF;
    }

}
