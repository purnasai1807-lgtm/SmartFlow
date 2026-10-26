package com.smartflow.service;

import com.smartflow.dao.RecordDao;
import com.smartflow.dao.ResourceDao;
import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.exception.ValidationException;
import com.smartflow.model.Resource;
import com.smartflow.model.SystemRecord;
import com.smartflow.model.User;
import com.smartflow.util.Validator;

import java.util.List;

/**
 * Resource service implementation with validation and auditing.
 */
public class ResourceServiceImpl implements ResourceService {
    private final ResourceDao resourceDao;
    private final RecordDao recordDao;

    public ResourceServiceImpl(ResourceDao resourceDao, RecordDao recordDao) {
        this.resourceDao = resourceDao;
        this.recordDao = recordDao;
    }

    @Override
    public List<Resource> getAllResources() throws DatabaseOperationException {
        return resourceDao.findAll();
    }

    @Override
    public Resource getResourceById(int id) throws DatabaseOperationException {
        return resourceDao.findById(id);
    }

    @Override
    public void createResource(Resource resource, User actor) throws ValidationException, DatabaseOperationException {
        Validator.requireNonEmpty(resource.getName(), "Resource Name");
        Validator.requirePositive(resource.getTotalCapacity(), "Capacity");
        Validator.requireNonNegative(resource.getHourlyCost(), "Hourly Cost");

        int newId = resourceDao.save(resource);

        String actorName = actor != null ? actor.getUsername() : "SYSTEM";
        recordDao.log(new SystemRecord("RESOURCE_CREATED", "RESOURCE", newId,
                "Created resource '" + resource.getName() + "' (Capacity: " + resource.getTotalCapacity() + ")", actorName));
    }

    @Override
    public void updateResource(Resource resource, User actor) throws ValidationException, DatabaseOperationException {
        Validator.requireNonEmpty(resource.getName(), "Resource Name");
        Validator.requirePositive(resource.getTotalCapacity(), "Capacity");
        Validator.requireNonNegative(resource.getHourlyCost(), "Hourly Cost");

        resourceDao.update(resource);

        String actorName = actor != null ? actor.getUsername() : "SYSTEM";
        recordDao.log(new SystemRecord("RESOURCE_UPDATED", "RESOURCE", resource.getId(),
                "Updated resource '" + resource.getName() + "'", actorName));
    }

    @Override
    public void deleteResource(int id, User actor) throws DatabaseOperationException {
        Resource r = resourceDao.findById(id);
        resourceDao.delete(id);

        String actorName = actor != null ? actor.getUsername() : "SYSTEM";
        recordDao.log(new SystemRecord("RESOURCE_DELETED", "RESOURCE", id,
                "Deleted resource #" + id + (r != null ? " ('" + r.getName() + "')" : ""), actorName));
    }
}
