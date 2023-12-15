package ru.mipt1c.homework.task1;

import java.io.*;
import java.util.*;

import org.apache.commons.io.serialization.ValidatingObjectInputStream;

/**
 * A simple disk storage. All the data is stored in one file without any ordering.
 */
public class FileStorage<K, V> {
    private File file;
    private ObjectOutputStream writeStream = null;
    
    private FileStorage(File file, boolean isRo) {
        this.file = file;
        boolean append = true;
        if (!file.exists()) {
            append = false;
            try {
                file.createNewFile();
                file.setWritable(true);
                file.setReadable(true);
            } catch (IOException _e) {
                throw new RuntimeException("FileStorage: cannot create file in constructor");
            }
        }
        try {
            if (!isRo) {
                if (!append) {
                    writeStream = new ObjectOutputStream(new FileOutputStream(file));
                } else {
                    writeStream = new AppendingObjectOutputStream(new FileOutputStream(file, true));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("FileStorage: cannot create stream in constructor");
        }
    }

    public FileStorage(File file) {
        this(file, false);
    }

    /**
     * Returns an iterator over all the data.
     * Data format is validated during the iteration.
     */
    public Iterator<Node<K, V>> iterator() {
        try {
            InputStream fileStream = new FileInputStream(file);
            ValidatingObjectInputStream stream = new ValidatingObjectInputStream(fileStream)
                    .accept("ru.mipt1c.*").accept(java.util.Date.class)
                    .accept("java.lang.*");
            return new Iterator<Node<K, V>>() {
                private Node<K, V> last = null;
                @Override
                public boolean hasNext() {
                    try {
                        Object obj = stream.readObject();
                        last = (Node<K, V>) obj;
                        if (last == null) {
                            return false;
                        }
                        return true;
                    } catch (EOFException _e) {
                        /* We have iterated over all the data. */
                        return false;
                    } catch (ClassNotFoundException | IOException e) {
                        throw new RuntimeException("File storage: iterator.hasNext failed: " + e.getMessage());
                    }
                }

                @Override
                public Node<K, V> next() {
                    return last;
                }
            };
        } catch (IOException e) {
            return new Iterator<Node<K, V>>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Node<K, V> next() {
                    return null;
                }
            };
        }
    }

    public void append(K key, V value) {
        assert (file.exists());
        assert (writeStream != null);
        try {
            writeStream.writeObject(new Node<K, V>(key, value));
            writeStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("FileStorage: cannot write to file in append: " + e.getMessage());
        }
    }

    /**
     * Saves the file storage to another file and clears it.
     * Return FileStorage with old data - do not forget to drop it!
     */
    public FileStorage<K, V> shed() {
        String originalName = file.getAbsolutePath();
        try {
            if (writeStream != null) {
                writeStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("FileStorage: connot close write stream in shed: " + e.getMessage());
        }
        writeStream = null;
        File oldDataFile = new File(originalName + "_old");
        boolean rc = file.renameTo(oldDataFile);
        if (!rc) {
            throw new RuntimeException("FileStorage: connot rename file in shed");
        }
        FileStorage<K, V> oldData = new FileStorage<K, V>(oldDataFile, true);
        file = new File(originalName);
        /* Renamed file still exists sometimes. Delete it just for sure. */
        if (file.exists()) {
            rc = file.delete();
            if (!rc) {
                throw new RuntimeException("FileStorage: connot delete file in shed");
            }
        }
        try {
            file.createNewFile();
            file.setWritable(true);
            file.setReadable(true);
        } catch (IOException _e) {
            throw new RuntimeException("FileStorage: connot create file in shed");
        }
        try {
            writeStream = new ObjectOutputStream(new FileOutputStream(file, true));
        } catch (IOException e) {
            throw new RuntimeException("FileStorage: cannot create write stream in shed: " + e.getMessage());
        }
        return oldData;
    }

    public void drop() {
        boolean rc = file.delete();
        if (!rc) {
            throw new RuntimeException("FileStorage: connot create file in drop");
        }
        if (writeStream != null) {
            try {
                writeStream.close();
            } catch (IOException e) {
                throw new RuntimeException("FileStorage: connot close stream in drop");
            }
        }
        file = null;
        writeStream = null;
    }

    public void close() {
        if (writeStream != null) {
            try {
                writeStream.close();
            } catch (IOException e) {
                throw new RuntimeException("FileStorage: connot close stream in close");
            }
        }
        file = null;
        writeStream = null;
    }
}
