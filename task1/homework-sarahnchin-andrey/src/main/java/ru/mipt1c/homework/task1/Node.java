package ru.mipt1c.homework.task1;

import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.Getter;
import java.io.Serializable;

@AllArgsConstructor
public class Node<K, V> implements Serializable {
    @Setter
    @Getter
    private K key;

    @Setter
    @Getter
    private V value;
}
