package com.smartflow.service;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.exception.ResourceConflictException;
import com.smartflow.exception.ValidationException;
import com.smartflow.model.ConflictReport;
import com.smartflow.model.Resource;
import com.smartflow.model.Task;
import com.smartflow.model.User;

import java.util.List;
import java.util.PriorityQueue;

/**
 * Service Interface defining business operations for Tasks,
 * resource conflict detection, and reliable allocation.
 */
public interface TaskService {
    List<Task> getAllTasks() throws DatabaseOperationException;
    Task getTaskById(int id) throws DatabaseOperationException;
    void createTask(Task task, User actor) throws ValidationException, ResourceConflictException, DatabaseOperationException;
    void updateTask(Task task, User actor) throws ValidationException, ResourceConflictException, DatabaseOperationException;
    void deleteTask(int taskId, User actor) throws DatabaseOperationException;
    PriorityQueue<Task> getPriorityTaskQueue() throws DatabaseOperationException;

    // Conflict detection & reliable allocation methods
    List<Resource> findAlternativeResources(int unavailableResourceId) throws DatabaseOperationException;
    String checkAllocationAdvisory(Task task) throws DatabaseOperationException;
    ConflictReport scanAllConflicts() throws DatabaseOperationException;
    int reconcileResourceAllocations(User actor) throws DatabaseOperationException;
}
