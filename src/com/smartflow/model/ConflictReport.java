package com.smartflow.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model encapsulating resource conflicts, workload bottlenecks,
 * and allocation diagnostics.
 */
public class ConflictReport {
    private final List<String> resourceConflicts = new ArrayList<>();
    private final List<String> workloadConflicts = new ArrayList<>();
    private final List<String> deadlineConflicts = new ArrayList<>();
    private int reconciledAllocationCount = 0;

    public void addResourceConflict(String conflict) {
        resourceConflicts.add(conflict);
    }

    public void addWorkloadConflict(String conflict) {
        workloadConflicts.add(conflict);
    }

    public void addDeadlineConflict(String conflict) {
        deadlineConflicts.add(conflict);
    }

    public List<String> getResourceConflicts() {
        return resourceConflicts;
    }

    public List<String> getWorkloadConflicts() {
        return workloadConflicts;
    }

    public List<String> getDeadlineConflicts() {
        return deadlineConflicts;
    }

    public boolean hasAnyConflicts() {
        return !resourceConflicts.isEmpty() || !workloadConflicts.isEmpty() || !deadlineConflicts.isEmpty();
    }

    public int getTotalConflictCount() {
        return resourceConflicts.size() + workloadConflicts.size() + deadlineConflicts.size();
    }

    public int getReconciledAllocationCount() {
        return reconciledAllocationCount;
    }

    public void setReconciledAllocationCount(int reconciledAllocationCount) {
        this.reconciledAllocationCount = reconciledAllocationCount;
    }
}
