package com.smartflow.service;

import com.smartflow.dao.RecordDao;
import com.smartflow.dao.ResourceDao;
import com.smartflow.dao.TaskDao;
import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.exception.ResourceConflictException;
import com.smartflow.exception.ValidationException;
import com.smartflow.model.ConflictReport;
import com.smartflow.model.Resource;
import com.smartflow.model.SystemRecord;
import com.smartflow.model.Task;
import com.smartflow.model.User;
import com.smartflow.util.Validator;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation orchestrating business rules, validations,
 * resource conflict detection, reliable allocation, and audit trails.
 */
public class TaskServiceImpl implements TaskService {
    private final TaskDao taskDao;
    private final ResourceDao resourceDao;
    private final RecordDao recordDao;

    public TaskServiceImpl(TaskDao taskDao, ResourceDao resourceDao, RecordDao recordDao) {
        this.taskDao = taskDao;
        this.resourceDao = resourceDao;
        this.recordDao = recordDao;
    }

    @Override
    public List<Task> getAllTasks() throws DatabaseOperationException {
        return taskDao.findAll();
    }

    @Override
    public Task getTaskById(int id) throws DatabaseOperationException {
        return taskDao.findById(id);
    }

    @Override
    public void createTask(Task task, User actor) throws ValidationException, ResourceConflictException, DatabaseOperationException {
        // Input validation
        Validator.requireNonEmpty(task.getTitle(), "Task Title");
        Validator.validateTaskDates(task.getDueDate());

        String actorName = actor != null ? actor.getUsername() : "SYSTEM";

        // Resource Conflict Detection & Allocation
        if (task.getResourceId() > 0 && !task.isCompleted()) {
            Resource res = resourceDao.findById(task.getResourceId());
            if (res != null) {
                if (!res.isAvailable(1)) {
                    // Record conflict in audit trail
                    recordDao.log(new SystemRecord("RESOURCE_CONFLICT", "RESOURCE", res.getId(),
                            "Conflict blocked: Resource '" + res.getName() + "' is fully booked ("
                                    + res.getAllocatedQuantity() + "/" + res.getTotalCapacity() + " allocated).", actorName));

                    List<Resource> alternatives = findAlternativeResources(res.getId());
                    String altMsg = alternatives.isEmpty()
                            ? "No alternative resources available."
                            : "Suggested available alternatives: " + alternatives.stream()
                            .map(r -> r.getName() + " (" + r.getAvailableQuantity() + " avail)")
                            .collect(Collectors.joining(", "));

                    throw new ResourceConflictException(res.getId(),
                            "Resource '" + res.getName() + "' has 0 capacity available. " + altMsg);
                }

                resourceDao.adjustAllocation(res.getId(), 1);
                recordDao.log(new SystemRecord("RESOURCE_ALLOCATED", "RESOURCE", res.getId(),
                        "Allocated 1 unit of '" + res.getName() + "' to new task '" + task.getTitle() + "'", actorName));
            }
        }

        // Workload conflict detection for assigned member
        if (task.getAssignedUserId() > 0 && !task.isCompleted()) {
            long activeTasks = taskDao.findAll().stream()
                    .filter(t -> t.getAssignedUserId() == task.getAssignedUserId() && !t.isCompleted())
                    .count();
            if (activeTasks >= 3) {
                recordDao.log(new SystemRecord("WORKLOAD_WARNING", "USER", task.getAssignedUserId(),
                        "Task assigned to user with high active workload (" + (activeTasks + 1) + " active tasks).", actorName));
            }
        }

        int newId = taskDao.save(task);

        // Audit Logging for task creation
        recordDao.log(new SystemRecord("TASK_CREATED", "TASK", newId,
                "Created task '" + task.getTitle() + "' [Priority: " + task.getPriority()
                        + (task.getResourceId() > 0 ? ", Resource: " + task.getResourceName() : "")
                        + (task.getAssignedUserId() > 0 ? ", Assignee: " + task.getAssignedUserName() : "") + "]", actorName));
    }

    @Override
    public void updateTask(Task task, User actor) throws ValidationException, ResourceConflictException, DatabaseOperationException {
        Validator.requireNonEmpty(task.getTitle(), "Task Title");
        Validator.validateTaskDates(task.getDueDate());

        String actorName = actor != null ? actor.getUsername() : "SYSTEM";
        Task existing = taskDao.findById(task.getId());

        if (existing != null) {
            boolean oldHadActiveRes = existing.getResourceId() > 0 && !existing.isCompleted();
            boolean newNeedsActiveRes = task.getResourceId() > 0 && !task.isCompleted();

            if (existing.getResourceId() != task.getResourceId()) {
                // Old resource released if it was active
                if (oldHadActiveRes) {
                    resourceDao.adjustAllocation(existing.getResourceId(), -1);
                    recordDao.log(new SystemRecord("RESOURCE_RELEASED", "RESOURCE", existing.getResourceId(),
                            "Released 1 unit due to task #" + task.getId() + " resource change", actorName));
                }

                // New resource allocated if task is active
                if (newNeedsActiveRes) {
                    Resource newRes = resourceDao.findById(task.getResourceId());
                    if (newRes != null && !newRes.isAvailable(1)) {
                        recordDao.log(new SystemRecord("RESOURCE_CONFLICT", "RESOURCE", newRes.getId(),
                                "Conflict blocked: Resource '" + newRes.getName() + "' is fully booked on task update.", actorName));

                        List<Resource> alternatives = findAlternativeResources(newRes.getId());
                        String altMsg = alternatives.isEmpty() ? "" : " Alternatives: " + alternatives.stream()
                                .map(r -> r.getName() + " (" + r.getAvailableQuantity() + " avail)")
                                .collect(Collectors.joining(", "));

                        throw new ResourceConflictException(newRes.getId(),
                                "Resource '" + newRes.getName() + "' is fully allocated." + altMsg);
                    }
                    if (newRes != null) {
                        resourceDao.adjustAllocation(newRes.getId(), 1);
                        recordDao.log(new SystemRecord("RESOURCE_ALLOCATED", "RESOURCE", newRes.getId(),
                                "Allocated 1 unit of '" + newRes.getName() + "' to task #" + task.getId(), actorName));
                    }
                }
            } else {
                // Resource ID is same, check lifecycle status transitions
                if (oldHadActiveRes && task.isCompleted()) {
                    // Task completed: automatically release the resource
                    resourceDao.adjustAllocation(task.getResourceId(), -1);
                    recordDao.log(new SystemRecord("TASK_COMPLETED", "TASK", task.getId(),
                            "Task completed. Automatically released resource #" + task.getResourceId(), actorName));
                    recordDao.log(new SystemRecord("RESOURCE_RELEASED", "RESOURCE", task.getResourceId(),
                            "Released 1 unit upon task #" + task.getId() + " completion", actorName));
                } else if (!oldHadActiveRes && newNeedsActiveRes) {
                    // Task reopened from completed to active: re-check capacity
                    Resource res = resourceDao.findById(task.getResourceId());
                    if (res != null && !res.isAvailable(1)) {
                        recordDao.log(new SystemRecord("RESOURCE_CONFLICT", "RESOURCE", res.getId(),
                                "Conflict blocked: Resource '" + res.getName() + "' occupied while attempting to reopen task #" + task.getId(), actorName));
                        throw new ResourceConflictException(res.getId(),
                                "Cannot reopen task: Resource '" + res.getName() + "' is now fully occupied by other tasks.");
                    }
                    if (res != null) {
                        resourceDao.adjustAllocation(res.getId(), 1);
                        recordDao.log(new SystemRecord("RESOURCE_ALLOCATED", "RESOURCE", res.getId(),
                                "Re-allocated 1 unit of '" + res.getName() + "' upon task reopening", actorName));
                    }
                }
            }
        }

        taskDao.update(task);

        recordDao.log(new SystemRecord("TASK_UPDATED", "TASK", task.getId(),
                "Updated task '" + task.getTitle() + "' -> Status: " + task.getStatus()
                        + ", Progress: " + task.getProgress() + "%", actorName));
    }

    @Override
    public void deleteTask(int taskId, User actor) throws DatabaseOperationException {
        Task task = taskDao.findById(taskId);
        String actorName = actor != null ? actor.getUsername() : "SYSTEM";

        if (task != null && task.getResourceId() > 0 && !task.isCompleted()) {
            resourceDao.adjustAllocation(task.getResourceId(), -1);
            recordDao.log(new SystemRecord("RESOURCE_RELEASED", "RESOURCE", task.getResourceId(),
                    "Released resource due to deletion of active task #" + taskId, actorName));
        }

        taskDao.delete(taskId);

        recordDao.log(new SystemRecord("TASK_DELETED", "TASK", taskId,
                "Deleted task ID " + taskId + (task != null ? " ('" + task.getTitle() + "')" : ""), actorName));
    }

    @Override
    public PriorityQueue<Task> getPriorityTaskQueue() throws DatabaseOperationException {
        Comparator<Task> taskComparator = Comparator
                .comparing((Task t) -> t.getPriority().getRank(), Comparator.reverseOrder())
                .thenComparing(Task::getDueDate);

        PriorityQueue<Task> queue = new PriorityQueue<>(taskComparator);
        List<Task> all = taskDao.findAll();
        for (Task t : all) {
            if (!t.isCompleted()) {
                queue.offer(t);
            }
        }
        return queue;
    }

    @Override
    public List<Resource> findAlternativeResources(int unavailableResourceId) throws DatabaseOperationException {
        Resource target = resourceDao.findById(unavailableResourceId);
        if (target == null) {
            return Collections.emptyList();
        }

        return resourceDao.findAll().stream()
                .filter(r -> r.getId() != unavailableResourceId
                        && r.getType() == target.getType()
                        && r.getAvailableQuantity() > 0)
                .sorted(Comparator.comparingInt(Resource::getAvailableQuantity).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public String checkAllocationAdvisory(Task task) throws DatabaseOperationException {
        if (task == null) return null;

        // Resource capacity warning
        if (task.getResourceId() > 0 && !task.isCompleted()) {
            Resource res = resourceDao.findById(task.getResourceId());
            if (res != null && !res.isAvailable(1)) {
                return "RESOURCE CONFLICT: '" + res.getName() + "' is at 100% capacity ("
                        + res.getAllocatedQuantity() + "/" + res.getTotalCapacity() + " allocated).";
            }
        }

        // Workload conflict advisory
        if (task.getAssignedUserId() > 0 && !task.isCompleted()) {
            long activeCount = taskDao.findAll().stream()
                    .filter(t -> t.getAssignedUserId() == task.getAssignedUserId()
                            && !t.isCompleted()
                            && t.getId() != task.getId())
                    .count();
            if (activeCount >= 3) {
                return "WORKLOAD ADVISORY: Assignee currently has " + activeCount + " active tasks in progress.";
            }
        }

        return null;
    }

    @Override
    public ConflictReport scanAllConflicts() throws DatabaseOperationException {
        ConflictReport report = new ConflictReport();
        List<Task> allTasks = taskDao.findAll();
        List<Resource> allResources = resourceDao.findAll();

        // 1. Resource Over-Allocation conflicts
        for (Resource res : allResources) {
            if (res.getAllocatedQuantity() > res.getTotalCapacity()) {
                report.addResourceConflict("Resource '" + res.getName() + "' is OVER-ALLOCATED: "
                        + res.getAllocatedQuantity() + " assigned vs " + res.getTotalCapacity() + " maximum capacity.");
            }
        }

        // 2. Member Workload bottlenecks (>= 3 active tasks)
        Map<String, Long> userWorkloads = allTasks.stream()
                .filter(t -> !t.isCompleted() && t.getAssignedUserName() != null && !t.getAssignedUserName().isEmpty())
                .collect(Collectors.groupingBy(Task::getAssignedUserName, Collectors.counting()));

        userWorkloads.forEach((userName, count) -> {
            if (count >= 3) {
                report.addWorkloadConflict("Member '" + userName + "' has high workload (" + count + " active tasks).");
            }
        });

        // 3. User deadline collision (same user with multiple high/critical tasks due on same day)
        Map<String, List<Task>> userDateTasks = allTasks.stream()
                .filter(t -> !t.isCompleted() && t.getAssignedUserName() != null && t.getDueDate() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignedUserName() + " | " + t.getDueDate()));

        userDateTasks.forEach((key, tasks) -> {
            if (tasks.size() > 1) {
                report.addDeadlineConflict("Deadline Collision: " + key + " has " + tasks.size() + " concurrent deadlines.");
            }
        });

        return report;
    }

    @Override
    public int reconcileResourceAllocations(User actor) throws DatabaseOperationException {
        List<Task> allTasks = taskDao.findAll();
        List<Resource> allResources = resourceDao.findAll();

        // Count actual active non-completed allocations per resource using Java Stream API
        Map<Integer, Long> actualActiveUsage = allTasks.stream()
                .filter(t -> !t.isCompleted() && t.getResourceId() > 0)
                .collect(Collectors.groupingBy(Task::getResourceId, Collectors.counting()));

        int reconciledCount = 0;
        for (Resource r : allResources) {
            int actual = actualActiveUsage.getOrDefault(r.getId(), 0L).intValue();
            if (r.getAllocatedQuantity() != actual) {
                r.setAllocatedQuantity(actual);
                resourceDao.update(r);
                reconciledCount++;
            }
        }

        if (reconciledCount > 0) {
            String actorName = actor != null ? actor.getUsername() : "SYSTEM";
            recordDao.log(new SystemRecord("ALLOCATION_RECONCILED", "SYSTEM", 0,
                    "Reconciliation engine corrected " + reconciledCount + " drifting resource allocation counters.", actorName));
        }

        return reconciledCount;
    }
}
