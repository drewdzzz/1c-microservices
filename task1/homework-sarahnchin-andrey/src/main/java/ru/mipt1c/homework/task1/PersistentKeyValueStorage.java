package ru.mipt1c.homework.task1;

import java.io.*;
import java.util.*;

public class PersistentKeyValueStorage<K, V> implements KeyValueStorage<K, V> {
    private final File baseDir;
    private final String storageFileName = "Data";
    private FileStorage<K, V> fileStorage;
    /**
     * In-memory storage for insertions and replaces.
     */
    private Map<K, V> memLevel;
    /**
     * In-memory storage for deletions.
     */
    private Set<K> cemetery;
    private boolean isClosed;
    private int size = 0;
    private long epoch = 0;

    public PersistentKeyValueStorage(String baseDir) throws MalformedDataException {
        this.baseDir = new File(baseDir);
        if (!this.baseDir.exists()) {
            this.baseDir.mkdir();
        }
        fileStorage = new FileStorage<>(new File(this.baseDir, storageFileName));
        checkData();
        memLevel = new HashMap<>();
        cemetery = new HashSet<>();
    }

    @Override
    public V read(K key) {
        checkClosed();
        if (cemetery.contains(key)) {
            return null;
        }
        if (memLevel.containsKey(key)) {
            return memLevel.get(key);
        }
        Iterator<Node<K, V>> it = fileStorage.iterator();
        while (it.hasNext()) {
            Node<K, V> node = it.next();
            assert (node != null);
            if (node.getKey().equals(key)) {
                return node.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean exists(K key) {
        return read(key) != null;
    }

    @Override
    public void write(K key, V value) {
        checkClosed();
        bumpEpoch(key, false);
        memLevel.put(key, value);
        if (cemetery.contains(key)) {
            cemetery.remove(key);
        }
    }

    @Override
    public void delete(K key) {
        checkClosed();
        bumpEpoch(key, true);
        if (memLevel.containsKey(key)) {
            memLevel.remove(key);
        }
        cemetery.add(key);
    }

    @Override
    public Iterator<K> readKeys() {
        checkClosed();
        Iterator<Node<K, V>> fileIt = fileStorage.iterator();
        Iterator<K> memIt = memLevel.keySet().iterator();
        return new Iterator<K>() {
            private long savedEpoch = epoch;
            private Node<K, V> last;

            @Override
            public boolean hasNext() {
                if (savedEpoch != epoch) {
                    throw new java.util.ConcurrentModificationException();
                }
                while (fileIt.hasNext()) {
                    last = fileIt.next();
                    if (last == null) {
                        break;
                    }
                    if (cemetery.contains(last.getKey()) || memLevel.containsKey(last.getKey())) {
                        /* Skip all deleted and replaced keys. */
                        continue;
                    }
                    return true;
                }
                return memIt.hasNext();
            }

            @Override
            public K next() {
                if (savedEpoch != epoch) {
                    throw new java.util.ConcurrentModificationException();
                }
                if (last == null) {
                    return memIt.next();
                }
                return last.getKey();
            }
        };
    }

    @Override
    public void close() {
        checkClosed();
        flush();
        fileStorage.close();
        fileStorage = null;
        isClosed = true;
    }

    @Override
    public int size() {
        checkClosed();
        flush();
        return size;
    }

    @Override
    public void flush() {
        checkClosed();
        size = 0;
        FileStorage<K, V> oldData = fileStorage.shed();
        /* Replaces and deletes. */
        Iterator<Node<K, V>> it = oldData.iterator();
        while (it.hasNext()) {
            Node<K, V> curr = it.next();
            if (cemetery.contains(curr.getKey())) {
                cemetery.remove(curr.getKey());
                continue;
            }
            if (memLevel.containsKey(curr.getKey())) {
                curr.setValue(memLevel.get(curr.getKey()));
                memLevel.remove(curr.getKey());
            }
            fileStorage.append(curr.getKey(), curr.getValue());
            this.size += 1;
        }
        oldData.drop();
        /* Insertions. */
        for (Map.Entry<K, V> entry : memLevel.entrySet()) {
            this.size += 1;
            fileStorage.append(entry.getKey(), entry.getValue());
        }
        memLevel.clear();
        cemetery.clear();
    }

    /**
     * Throws an exception if the storage is closed.
     */
    private void checkClosed() {
        if (isClosed) {
            throw new RuntimeException("The storage is closed");
        }
    }

    /**
     * Simply iterates over all the storage - validation is done by iterator.
     */
    private void checkData() throws MalformedDataException {
        try {
            Iterator<Node<K, V>> it = fileStorage.iterator();
            while (it.hasNext()) {
                it.next();
            }
        } catch (Exception e) {
            throw new MalformedDataException();
        }
    }

    /**
     * Conditionally bump current epoch. If epoch is bumped, all the iterators
     * are considered invalid.
     * Epoch is bumped only when keySet is changed.
     * @param key key from query.
     * @param isDelete is set if a query has delete type.
    */
    private void bumpEpoch(K key, boolean isDelete) {
        if (exists(key) == isDelete) {
            epoch += 1;
        }
    }
}
