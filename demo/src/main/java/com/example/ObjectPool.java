package com.example;

import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;

public class ObjectPool<T> {

    private final Queue<T> pool;
    private final int maxSize;

    public ObjectPool(int maxSize) {
        this.pool = new LinkedList<>();
        this.maxSize = maxSize;
    }

    public T acquireObject() {
        T object;
        synchronized (pool) {
            object = pool.poll();
        }
        if (object == null) {
            object = createObject();
        }
        return object;
    }

    public void releaseObject(T object) {
        synchronized (pool) {
            if (pool.size() < maxSize) {
                pool.offer(object);
            }
        }
    }

    protected T createObject() {
        // Implement logic to create a new instance of type T
        return (T) new HashMap<>(); // Example for HashMap
    }
}
