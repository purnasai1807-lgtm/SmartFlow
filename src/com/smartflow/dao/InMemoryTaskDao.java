package com.smartflow.dao;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.Task;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-Memory interchangeable implementation of TaskDao.
 * Uses Java Collections (ConcurrentHashMap, ArrayList) for rapid testing or offline mock mode.
 */
public class InMemoryTaskDao implements TaskDao {
    private final Map<Integer, Task> store = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(100);

    @Override
    public List<Task> findAll() throws DatabaseOperationException {
        return new ArrayList<>(store.values());
    }

    @Override
    public Task findById(int id) throws DatabaseOperationException {
        return store.get(id);
    }

    @Override
    public int save(Task task) throws DatabaseOperationException {
        int newId = idGenerator.incrementAndGet();
        task.setId(newId);
        store.put(newId, task);
        return newId;
    }

    @Override
    public boolean update(Task task) throws DatabaseOperationException {
        if (!store.containsKey(task.getId())) {
            return false;
        }
        store.put(task.getId(), task);
        return true;
    }

    @Override
    public boolean delete(int id) throws DatabaseOperationException {
        return store.remove(id) != null;
    }
}
