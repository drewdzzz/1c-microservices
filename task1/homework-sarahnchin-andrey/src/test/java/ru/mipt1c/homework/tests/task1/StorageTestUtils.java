package ru.mipt1c.homework.tests.task1;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StorageTestUtils {
    public static final ThreadLocal<Calendar> CALENDAR = ThreadLocal.withInitial(Calendar::getInstance);

    private StorageTestUtils() {
        // Cannot instantiate
    }

    public static void doInTempDirectory(Callback<String> callback) {
        try {
            Path path = null;
            try {
                path = Files.createTempDirectory(new File(".").toPath(), "test_task_2");
                callback.callback(path.toString());
            } finally {
                if (path != null) {
                    FileUtils.deleteDirectory(path.toFile());
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected Exception", e);
        }
    }

    public static Date date(int year, int month, int day) {
        Calendar calendar = CALENDAR.get();
        calendar.set(year, month, day);
        return calendar.getTime();
    }

    @SafeVarargs
    public static <T> void assertFullyMatch(Iterator<T> iterator, T... items) {
        assertFullyMatch(iterator, new HashSet<>(Arrays.asList(items)));
    }

    public static <T> void assertFullyMatch(Iterator<T> iterator, Set<T> set) {
        int count = 0;
        while (iterator.hasNext()) {
            T t = iterator.next();
            ++count;
            if (!set.contains(t)) {
                throw new AssertionError("Collections doesn't match");
            }
        }

        if (count != set.size()) {
            throw new AssertionError("Collections doesn't match");
        }
    }

    public static long measureTime(Measurable function) {
        long startTime = System.currentTimeMillis();
        function.doSomething();
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    @FunctionalInterface
    public interface Callback<T> {
        void callback(T t) throws Exception;
    }

    @FunctionalInterface
    public interface Measurable {
        void doSomething();
    }
}
