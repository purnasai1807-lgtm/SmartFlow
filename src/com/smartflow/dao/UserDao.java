package com.smartflow.dao;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.User;
import java.util.List;

/**
 * Data Access Object Interface for User entities.
 */
public interface UserDao {
    List<User> findAll() throws DatabaseOperationException;
    User findById(int id) throws DatabaseOperationException;
    User findByUsername(String username) throws DatabaseOperationException;
    int save(User user) throws DatabaseOperationException;
}
