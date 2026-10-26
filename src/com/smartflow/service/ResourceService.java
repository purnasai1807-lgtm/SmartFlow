package com.smartflow.service;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.exception.ValidationException;
import com.smartflow.model.Resource;
import com.smartflow.model.User;

import java.util.List;

/**
 * Service interface for Resource operations.
 */
public interface ResourceService {
    List<Resource> getAllResources() throws DatabaseOperationException;
    Resource getResourceById(int id) throws DatabaseOperationException;
    void createResource(Resource resource, User actor) throws ValidationException, DatabaseOperationException;
    void updateResource(Resource resource, User actor) throws ValidationException, DatabaseOperationException;
    void deleteResource(int id, User actor) throws DatabaseOperationException;
}
