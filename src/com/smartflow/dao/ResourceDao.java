package com.smartflow.dao;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.Resource;
import java.util.List;

/**
 * Data Access Object Interface for Resource entities.
 */
public interface ResourceDao {
    List<Resource> findAll() throws DatabaseOperationException;
    Resource findById(int id) throws DatabaseOperationException;
    int save(Resource resource) throws DatabaseOperationException;
    boolean update(Resource resource) throws DatabaseOperationException;
    boolean delete(int id) throws DatabaseOperationException;
    boolean adjustAllocation(int resourceId, int delta) throws DatabaseOperationException;
}
