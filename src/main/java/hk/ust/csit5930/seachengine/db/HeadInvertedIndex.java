package hk.ust.csit5930.seachengine.db;

import hk.ust.csit5930.seachengine.db.base.StringDBMap;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Component;

import java.util.TreeMap;

@Component
public class HeadInvertedIndex extends StringDBMap<TreeMap<Integer, Integer>> {

    public HeadInvertedIndex() throws RocksDBException {
        super("head_inverted_index");
    }

}
