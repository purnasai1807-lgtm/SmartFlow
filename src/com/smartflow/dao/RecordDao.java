package com.smartflow.dao;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.SystemRecord;
import java.util.List;

/**
 * Data Access Object Interface for System Audit Records.
 */
public interface RecordDao {
    List<SystemRecord> findAll() throws DatabaseOperationException;
    void log(SystemRecord record) throws DatabaseOperationException;
    void clearAll() throws DatabaseOperationException;
}
