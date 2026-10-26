package com.smartflow.service;

import com.smartflow.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service leveraging Java Stream API and Collections Framework.
 * Computes live operational statistics, workload distributions,
 * resource cost models, and project health indicators.
 */
public class AnalyticsService {

    /**
     * Groups and counts tasks by their lifecycle status using Stream API.
     */
    public Map<TaskStatus, Long> getTaskCountByStatus(List<Task> tasks) {
        Map<TaskStatus, Long> counts = tasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));

        // Ensure all statuses have a default of 0
        for (TaskStatus status : TaskStatus.values()) {
            counts.putIfAbsent(status, 0L);
        }
        return counts;
    }

    /**
     * Groups and counts tasks by priority level.
     */
    public Map<TaskPriority, Long> getTaskCountByPriority(List<Task> tasks) {
        Map<TaskPriority, Long> counts = tasks.stream()
                .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));

        for (TaskPriority priority : TaskPriority.values()) {
            counts.putIfAbsent(priority, 0L);
        }
        return counts;
    }

    /**
     * Calculates overall average progress percentage across active tasks.
     */
    public double getAverageProgress(List<Task> tasks) {
        return tasks.stream()
                .mapToInt(Task::getProgress)
                .average()
                .orElse(0.0);
    }

    /**
     * Filters tasks that are overdue.
     */
    public List<Task> getOverdueTasks(List<Task> tasks) {
        return tasks.stream()
                .filter(Task::isOverdue)
                .sorted(Comparator.comparing(Task::getDueDate))
                .collect(Collectors.toList());
    }

    /**
     * Filters tasks that are due within the next 48 hours.
     */
    public List<Task> getUpcomingDeadlines(List<Task> tasks) {
        return tasks.stream()
                .filter(Task::isDueSoon)
                .sorted(Comparator.comparing(Task::getDueDate))
                .collect(Collectors.toList());
    }

    /**
     * Calculates total burn rate (hourly cost) of currently allocated resources.
     */
    public double getTotalAllocatedResourceCost(List<Resource> resources) {
        return resources.stream()
                .mapToDouble(r -> r.getAllocatedQuantity() * r.getHourlyCost())
                .sum();
    }

    /**
     * Computes utilization percentage per resource.
     */
    public Map<String, Double> getResourceUtilization(List<Resource> resources) {
        return resources.stream()
                .collect(Collectors.toMap(
                        Resource::getName,
                        Resource::getUtilizationPercentage,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    /**
     * Counts active (non-completed) tasks assigned to each team member.
     */
    public Map<String, Long> getWorkloadPerUser(List<Task> tasks) {
        return tasks.stream()
                .filter(t -> !t.isCompleted() && t.getAssignedUserName() != null && !t.getAssignedUserName().isEmpty())
                .collect(Collectors.groupingBy(Task::getAssignedUserName, LinkedHashMap::new, Collectors.counting()));
    }

    /**
     * Sums total estimated effort hours per team member across active tasks.
     */
    public Map<String, Integer> getEstimatedHoursPerUser(List<Task> tasks) {
        return tasks.stream()
                .filter(t -> !t.isCompleted() && t.getAssignedUserName() != null && !t.getAssignedUserName().isEmpty())
                .collect(Collectors.groupingBy(
                        Task::getAssignedUserName,
                        LinkedHashMap::new,
                        Collectors.summingInt(Task::getEstimatedHours)
                ));
    }

    /**
     * Groups active hourly costs by ResourceType (Hardware, Cloud, Software, Workspace).
     */
    public Map<ResourceType, Double> getCostByResourceType(List<Resource> resources) {
        Map<ResourceType, Double> costMap = resources.stream()
                .collect(Collectors.groupingBy(
                        Resource::getType,
                        LinkedHashMap::new,
                        Collectors.summingDouble(r -> r.getAllocatedQuantity() * r.getHourlyCost())
                ));

        for (ResourceType type : ResourceType.values()) {
            costMap.putIfAbsent(type, 0.0);
        }
        return costMap;
    }

    /**
     * Counts bottleneck resources where utilization is 80% or greater.
     */
    public long getBottleneckResourceCount(List<Resource> resources) {
        return resources.stream()
                .filter(r -> r.getTotalCapacity() > 0 && r.getUtilizationPercentage() >= 80.0)
                .count();
    }

    /**
     * Computes overall project health score (0% to 100%).
     * Factors in average progress and penalizes overdue tasks.
     */
    public int getProjectHealthScore(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return 100;
        }

        double avgProgress = getAverageProgress(tasks);
        long overdueCount = getOverdueTasks(tasks).size();
        double overduePenalty = (overdueCount * 20.0) / tasks.size();

        double score = avgProgress - overduePenalty;
        return (int) Math.max(0, Math.min(100, Math.round(score)));
    }

    /**
     * Computes the distribution of audit actions for analytics reporting.
     */
    public Map<String, Long> getAuditActionDistribution(List<SystemRecord> records) {
        return records.stream()
                .collect(Collectors.groupingBy(SystemRecord::getAction, LinkedHashMap::new, Collectors.counting()));
    }
}
