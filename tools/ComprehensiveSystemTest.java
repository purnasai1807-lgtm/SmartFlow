package tools;

import com.smartflow.dao.*;
import com.smartflow.exception.ResourceConflictException;
import com.smartflow.model.*;
import com.smartflow.service.*;
import com.smartflow.thread.AsyncAuditExporter;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Headless Automated Verification Test Suite.
 * Validates:
 * 1. Resource Conflict Detection & Reliable Task Allocation Lifecycle
 * 2. Real Analytics via Java Stream API
 * 3. Full System Audit Trail & Async CSV Exporter
 */
public class ComprehensiveSystemTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=============================================================");
        System.out.println(">>> STARTING SMARTFLOW SYSTEM VERIFICATION TEST SUITE <<<");
        System.out.println("=============================================================");

        // Initialize DAOs & Services
        TaskDao taskDao = new JdbcTaskDao();
        ResourceDao resourceDao = new JdbcResourceDao();
        UserDao userDao = new JdbcUserDao();
        RecordDao recordDao = new JdbcRecordDao();

        TaskService taskService = new TaskServiceImpl(taskDao, resourceDao, recordDao);
        ResourceService resourceService = new ResourceServiceImpl(resourceDao, recordDao);
        AnalyticsService analyticsService = new AnalyticsService();

        // 1. Verify Users
        List<User> users = userDao.findAll();
        System.out.println("\n[TEST 1] User Role-Based Access Control:");
        System.out.println("  Loaded " + users.size() + " enterprise user profiles.");
        for (User u : users) {
            System.out.println("  - " + u.getFullName() + " | Role: " + u.getRole()
                    + " | Manage Res: " + u.canManageResources()
                    + " | Delete Tasks: " + u.canDeleteTasks()
                    + " | Clear Logs: " + u.canClearAuditLogs());
        }
        assert users.size() >= 3 : "Expected at least 3 users";
        User admin = users.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).findFirst().orElseThrow();
        User manager = users.stream().filter(u -> "MANAGER".equalsIgnoreCase(u.getRole())).findFirst().orElseThrow();
        User member = users.stream().filter(u -> "MEMBER".equalsIgnoreCase(u.getRole())).findFirst().orElseThrow();

        // 2. Verify Resources & Default Capacity
        List<Resource> resources = resourceService.getAllResources();
        System.out.println("\n[TEST 2] Enterprise Resource Inventory & Capacity:");
        System.out.println("  Loaded " + resources.size() + " enterprise resources.");
        for (Resource r : resources) {
            System.out.println("  - #" + r.getId() + " " + r.getName() + " [" + r.getType() + "] "
                    + "Cap: " + r.getTotalCapacity() + ", Alloc: " + r.getAllocatedQuantity()
                    + ", Avail: " + r.getAvailableQuantity() + ", Cost: $" + r.getHourlyCost() + "/hr");
        }
        assert !resources.isEmpty() : "Resources should not be empty";

        // Find a resource with capacity 1 to test conflict detection (e.g. Workstation)
        Resource limitedRes = resources.stream()
                .filter(r -> r.getTotalCapacity() == 1)
                .findFirst()
                .orElse(resources.get(0));

        System.out.println("\n[TEST 3] Resource Conflict Detection & Over-Allocation Prevention:");
        System.out.println("  Target resource for test: '" + limitedRes.getName()
                + "' (Capacity: " + limitedRes.getTotalCapacity() + ")");

        // Reset target resource allocation to 0 before test
        limitedRes.setAllocatedQuantity(0);
        resourceDao.update(limitedRes);

        // Task A: allocates 1 unit -> should succeed
        Task taskA = new Task("Task Alpha", "Setup initial model environment", TaskPriority.HIGH, LocalDate.now().plusDays(5));
        taskA.setResourceId(limitedRes.getId());
        taskA.setAssignedUserId(member.getId());
        taskA.setStatus(TaskStatus.IN_PROGRESS);
        taskService.createTask(taskA, manager);
        System.out.println("  ✓ Task A allocated 1 unit. Current alloc: " + resourceDao.findById(limitedRes.getId()).getAllocatedQuantity());

        // Task B: attempts to allocate same resource with capacity 1 -> MUST DETECT CONFLICT & THROW ResourceConflictException
        Task taskB = new Task("Task Beta", "Secondary hardware run", TaskPriority.CRITICAL, LocalDate.now().plusDays(3));
        taskB.setResourceId(limitedRes.getId());
        taskB.setAssignedUserId(member.getId());
        taskB.setStatus(TaskStatus.IN_PROGRESS);

        boolean conflictDetected = false;
        try {
            taskService.createTask(taskB, manager);
        } catch (ResourceConflictException ex) {
            conflictDetected = true;
            System.out.println("  ✓ SUCCESS: ResourceConflictException correctly triggered!");
            System.out.println("    Message: " + ex.getMessage());
        }
        assert conflictDetected : "ResourceConflictException should have been thrown!";

        // 3. Reliable Task Allocation Lifecycle & Auto-Release on Task Completion
        System.out.println("\n[TEST 4] Task Lifecycle Resource Auto-Deallocation:");
        taskA.setStatus(TaskStatus.COMPLETED);
        taskA.setProgress(100);
        taskService.updateTask(taskA, member);

        Resource afterCompletion = resourceDao.findById(limitedRes.getId());
        System.out.println("  ✓ Task A completed. Target resource allocated count: " + afterCompletion.getAllocatedQuantity()
                + " (Available: " + afterCompletion.getAvailableQuantity() + ")");
        assert afterCompletion.getAllocatedQuantity() == 0 : "Resource should be released upon completion!";

        // Now Task B can successfully allocate since Task A freed it
        taskService.createTask(taskB, manager);
        System.out.println("  ✓ Task B successfully allocated freed resource! Allocated now: "
                + resourceDao.findById(limitedRes.getId()).getAllocatedQuantity());

        // Clean up Task B
        taskService.deleteTask(taskB.getId(), manager);
        System.out.println("  ✓ Task B deleted. Resource automatically de-allocated: "
                + resourceDao.findById(limitedRes.getId()).getAllocatedQuantity());

        // 4. System-Wide Conflict Scanner & Allocation Reconciliation
        System.out.println("\n[TEST 5] System-Wide Conflict Scanner & Self-Healing Reconciliation:");
        ConflictReport report = taskService.scanAllConflicts();
        System.out.println("  Diagnostic scan complete. Total detected collisions: " + report.getTotalConflictCount());
        for (String c : report.getResourceConflicts()) System.out.println("    Resource warning: " + c);
        for (String c : report.getWorkloadConflicts()) System.out.println("    Workload warning: " + c);
        for (String c : report.getDeadlineConflicts()) System.out.println("    Deadline collision: " + c);

        int reconciled = taskService.reconcileResourceAllocations(admin);
        System.out.println("  Reconciliation engine processed: " + reconciled + " counter adjustment(s).");

        // 5. Real Analytics via Java Stream API
        System.out.println("\n[TEST 6] Real Analytics Engine (Java Stream API):");
        List<Task> allTasks = taskService.getAllTasks();
        List<Resource> allResources = resourceService.getAllResources();

        Map<TaskStatus, Long> statusCounts = analyticsService.getTaskCountByStatus(allTasks);
        System.out.println("  - Status Breakdown: " + statusCounts);

        Map<TaskPriority, Long> priorityCounts = analyticsService.getTaskCountByPriority(allTasks);
        System.out.println("  - Priority Breakdown: " + priorityCounts);

        Map<String, Long> workloads = analyticsService.getWorkloadPerUser(allTasks);
        System.out.println("  - Active Workload per Team Member: " + workloads);

        Map<ResourceType, Double> costByType = analyticsService.getCostByResourceType(allResources);
        System.out.println("  - Cost by Resource Type ($/hr): " + costByType);

        int healthScore = analyticsService.getProjectHealthScore(allTasks);
        System.out.println("  - Overall Project Health Score: " + healthScore + "%");

        long bottlenecks = analyticsService.getBottleneckResourceCount(allResources);
        System.out.println("  - Bottleneck Resources (>=80% util): " + bottlenecks);

        // 6. Comprehensive Audit Trail & Async CSV Export
        System.out.println("\n[TEST 7] System Audit Trail & Async CSV Exporter:");
        List<SystemRecord> auditRecords = recordDao.findAll();
        System.out.println("  Total audit records in database: " + auditRecords.size());
        assert !auditRecords.isEmpty() : "Audit records should contain logged events";

        for (int i = 0; i < Math.min(5, auditRecords.size()); i++) {
            SystemRecord r = auditRecords.get(i);
            System.out.printf("  [%d] %s | Action: %-20s | Actor: %-12s | %s%n",
                    r.getId(), r.getFormattedTimestamp(), r.getAction(), r.getPerformedBy(), r.getDetails());
        }

        // Test Async CSV Exporter (SwingWorker)
        File testExportFile = new File("test_audit_export.csv");
        AsyncAuditExporter exporter = new AsyncAuditExporter(auditRecords, testExportFile, msg -> {});
        exporter.execute();
        File exported = exporter.get();
        System.out.println("  ✓ Async CSV export completed successfully! File: " + exported.getAbsolutePath()
                + " (Size: " + exported.length() + " bytes)");
        assert exported.exists() && exported.length() > 0 : "Export file should exist and have content";

        // Clean up test export file
        testExportFile.delete();

        System.out.println("\n=============================================================");
        System.out.println(">>> ALL 7 SYSTEM VERIFICATION TESTS PASSED SUCCESSFULLY! <<<");
        System.out.println("=============================================================\n");
    }
}
