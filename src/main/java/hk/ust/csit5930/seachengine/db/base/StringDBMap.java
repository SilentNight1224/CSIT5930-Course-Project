package hk.ust.csit5930.seachengine.db.base;

import org.rocksdb.RocksDBException;

import java.io.Serializable;

public class StringDBMap<V extends Serializable> extends AbstractDBMap<String, V> {

    public StringDBMap(String dbName) throws RocksDBException {
        super(dbName);
    }

    @Override
    public byte[] keyToBytes(String key) {
        return key.getBytes();
    }

    @Override
    public String bytesToKey(byte[] key) {
        return new String(key);
    }
}
