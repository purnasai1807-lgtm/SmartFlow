package com.smartflow.dao;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.Task;
import java.util.List;

/**
 * Data Access Object Interface for Task entities.
 * Defines contract for interchangeable persistence implementations.
 */
public interface TaskDao {
    List<Task> findAll() throws DatabaseOperationException;
    Task findById(int id) throws DatabaseOperationException;
    int save(Task task) throws DatabaseOperationException;
    boolean update(Task task) throws DatabaseOperationException;
    boolean delete(int id) throws DatabaseOperationException;
}
