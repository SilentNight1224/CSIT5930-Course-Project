package hk.ust.csit5930.seachengine.db;

import hk.ust.csit5930.seachengine.db.base.AbstractDBMap;
import hk.ust.csit5930.seachengine.db.base.StringDBMap;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class URLID extends StringDBMap<Integer> {
    private volatile Integer nextId = null;

    public URLID() throws RocksDBException {
        super("url_id");
    }

    public synchronized Integer getAndIncrementNextID() {
        nextId = getNextId();
        return nextId++;
    }

    public synchronized Integer getNextId() {
        if (nextId == null) {
            AbstractDBMap<String, Integer>.Iterator iterator = getIterator();
            iterator.seekToLast();
            if (!iterator.isValid()) {
                nextId = 0;
            } else {
                try {
                    nextId = iterator.value() + 1;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return nextId;
    }
}
