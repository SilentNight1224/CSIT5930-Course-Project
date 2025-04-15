package hk.ust.csit5930.seachengine.db.base;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.io.*;

public abstract class AbstractDBMap<K, V extends Serializable> {
    private static String DB_PATH = "db";
    private final RocksDB db;

    public abstract byte[] keyToBytes(K key);
    public abstract K bytesToKey(byte[] key);

    @SuppressWarnings({"ResultOfMethodCallIgnored", "resource"})
    public AbstractDBMap(String dbName) throws RocksDBException {
        RocksDB.loadLibrary();
        Options options = new Options();
        options.setCreateIfMissing(true);
        File file = new File(DB_PATH);
        if (!file.exists() && !file.isDirectory()) {
            file.mkdir();
        }
        db = RocksDB.open(file.getPath() + "/" + dbName);
    }

    @SuppressWarnings("unchecked")
    public V get(K key) {
        try {
            byte[] val = db.get(keyToBytes(key));
            return (V) deserialize(val);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public V put(K key, V value) {
        try {
            V prev = get(key);
            db.put(keyToBytes(key), serialize(value));
            return prev;
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public V delete(K key) throws RocksDBException, IOException {
        V prev = get(key);
        db.delete(keyToBytes(key));
        return prev;
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);
        objectOutputStream.writeObject(obj);
        objectOutputStream.close();
        return out.toByteArray();
    }

    public Object deserialize(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(in);
        try {
            return objectInputStream.readObject();
        } catch(ClassNotFoundException e) {
            return null;
        } finally {
            objectInputStream.close();
        }
    }

    public Iterator getIterator() {
        return new Iterator();
    }

    public class Iterator {
        private final RocksIterator it;

        public Iterator() {
            it = db.newIterator();
            it.seekToFirst();
        }

        public void next() {
            it.next();
        }

        public boolean isValid() {
            return it.isValid();
        }

        public K key() {
            return bytesToKey(it.key());
        }

        @SuppressWarnings("unchecked")
        public V value() throws IOException {
            return (V) deserialize(it.value());
        }

        public void seekToFirst() {
            it.seekToFirst();
        }

        public void seekToLast(){
            it.seekToLast();
        }
    }

}
