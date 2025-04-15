package hk.ust.csit5930.seachengine.db;

import hk.ust.csit5930.seachengine.db.base.StringDBMap;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Component;

import java.util.TreeMap;

@Component
public class BodyInvertedIndex extends StringDBMap<TreeMap<Integer, Integer>> {

    public BodyInvertedIndex() throws RocksDBException {
        super("body_inverted_index");
    }

}
