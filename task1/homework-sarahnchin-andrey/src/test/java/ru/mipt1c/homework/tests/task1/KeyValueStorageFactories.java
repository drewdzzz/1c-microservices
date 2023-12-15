package ru.mipt1c.homework.tests.task1;

import ru.mipt1c.homework.task1.KeyValueStorage;
import ru.mipt1c.homework.task1.MalformedDataException;

public abstract class KeyValueStorageFactories {
    protected abstract KeyValueStorage<String, String> buildStringsStorage(String path) throws MalformedDataException;

    protected abstract KeyValueStorage<Integer, Double> buildNumbersStorage(String path) throws MalformedDataException;

    protected abstract KeyValueStorage<StudentKey, Student> buildPojoStorage(String path) throws MalformedDataException;
}
