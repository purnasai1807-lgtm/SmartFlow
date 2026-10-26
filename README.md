# SmartFlow: Enterprise Task & Resource Management System

> **A High-Performance Desktop Productivity & Resource Allocation Suite in Java**  
> Incorporating Object-Oriented Domain Modeling, Role-Based Access Control, Custom Exception Architecture, Background Concurrency Daemons, Interactive Swing GUI with Vector 2D Graphics, Embedded Relational Persistence (SQLite), PriorityQueue Scheduling, and Java Stream Processing.

---

## 📌 Technical Architecture & Module Specifications (B.Tech CSE Core Java Syllabus)

| Module | Core Architecture & Syllabus Concept | Implementation & Key Source Files |
|---|---|---|
| **Domain Entity Architecture** | Encapsulated domain entities with private state, parametric constructors, and business validation. | Models: [`User`](src/com/smartflow/model/User.java), [`Task`](src/com/smartflow/model/Task.java), [`Resource`](src/com/smartflow/model/Resource.java), [`ConflictReport`](src/com/smartflow/model/ConflictReport.java), and [`SystemRecord`](src/com/smartflow/model/SystemRecord.java). |
| **Role-Based Access Control (RBAC)** | Abstract classes, inheritance, polymorphic permissions, and decoupled persistence interfaces. | Abstract [`User`](src/com/smartflow/model/User.java) extended by [`AdminUser`](src/com/smartflow/model/AdminUser.java), [`ManagerUser`](src/com/smartflow/model/ManagerUser.java), and [`MemberUser`](src/com/smartflow/model/MemberUser.java). Interchangeable DAO contracts: [`TaskDao`](src/com/smartflow/dao/TaskDao.java), [`ResourceDao`](src/com/smartflow/dao/ResourceDao.java), [`RecordDao`](src/com/smartflow/dao/RecordDao.java), and [`UserDao`](src/com/smartflow/dao/UserDao.java). |
| **Resource Conflict Detection & Reliable Allocation** | Custom checked exception hierarchy, capacity invariant checking, workload bottleneck advisories, lifecycle auto-release on completion, and alternative resource recommendation. | Checked exception: [`ResourceConflictException`](src/com/smartflow/exception/ResourceConflictException.java). Service engine: [`TaskServiceImpl`](src/com/smartflow/service/TaskServiceImpl.java). GUI: Live conflict badges in [`TaskDialog`](src/com/smartflow/gui/TaskDialog.java) and diagnostic scanner in [`ConflictScannerDialog`](src/com/smartflow/gui/ConflictScannerDialog.java). |
| **Fault Tolerance & Autonomous Concurrency** | Multithreading with autonomous daemon threads and non-blocking background workers. | Background threads: [`DeadlineMonitorDaemon`](src/com/smartflow/thread/DeadlineMonitorDaemon.java) (autonomous daemon thread) and [`AsyncAuditExporter`](src/com/smartflow/thread/AsyncAuditExporter.java) / [`AsyncReportExporter`](src/com/smartflow/thread/AsyncReportExporter.java) (`SwingWorker` with progress updates). |
| **Event-Driven Desktop Presentation & Vector 2D Graphics** | Swing layouts, event listeners (`DocumentListener`, `ActionListener`), and custom `Graphics2D` vector painting. | Swing views: [`MainFrame`](src/com/smartflow/gui/MainFrame.java), [`LoginFrame`](src/com/smartflow/gui/LoginFrame.java), [`TaskPanel`](src/com/smartflow/gui/TaskPanel.java), [`ResourcePanel`](src/com/smartflow/gui/ResourcePanel.java), and [`RecordsPanel`](src/com/smartflow/gui/RecordsPanel.java). Custom vector 2D charting: [`AnalyticsChartPanel`](src/com/smartflow/gui/AnalyticsChartPanel.java) (4 interactive views: Status, Workload, Priority/Health, and Costs). |
| **Real Analytics & System Audit Trail** | Java 8+ Stream API pipelines (`groupingBy`, `counting`, `summingDouble`, `average`), `PriorityQueue` scheduling, and complete audit logging. | Embedded SQLite database (`smartflow.db`) via [`DatabaseManager`](src/com/smartflow/dao/DatabaseManager.java). Stream analytics: [`AnalyticsService`](src/com/smartflow/service/AnalyticsService.java). Filterable audit trail in [`RecordsPanel`](src/com/smartflow/gui/RecordsPanel.java) with multithreaded CSV export. |

---

## ⚡ Key Highlights: Syllabus-Aligned Implementations

### 1. Resource Conflict Detection & More Reliable Task Allocation
* **Capacity Over-Allocation Guard**: Automatically verifies that remaining units `(totalCapacity - allocatedQuantity) >= 1` before allowing resource commitment. If a resource is fully booked, commitment is rejected with a checked `ResourceConflictException`.
* **Automated Suggestion of Alternatives**: When a resource has zero capacity, the engine uses Stream API to locate alternative available resources sharing the same `ResourceType` and suggests them directly in the alert dialog.
* **Dynamic Lifecycle Resource Reclaim**: When a task moves to `COMPLETED`, its allocated resource unit is automatically de-allocated back to the resource pool. If reopened, capacity is verified before re-acquiring.
* **Member Workload Conflict Advisory**: When assigning a task, the system checks whether the selected team member already has $\ge 3$ active tasks or overlapping high-priority deadlines, warning the allocator.
* **System-Wide Conflict Scanner**: A dedicated diagnostic modal ([`ConflictScannerDialog`](src/com/smartflow/gui/ConflictScannerDialog.java)) scans all active tasks and resources, flags bottlenecks, and provides a 1-click **Self-Healing Reconciliation** engine to synchronize counters against the SQLite database.

### 2. Real Analytics & Complete System Audit Trail
* **Interactive 4-View Vector Graphics Canvas**: Built strictly with `java.awt.Graphics2D` and anti-aliasing hints:
  1. *Status & Resources*: Task volume bar chart + resource utilization donut chart.
  2. *Team Workload*: Active task distribution per team member with overload limit markers.
  3. *Priority & Health*: Task volume by priority (Critical/High/Medium/Low) + circular Project Health Gauge score (0–100%).
  4. *Cost Breakdown*: Hourly resource burn rate grouped by resource type.
* **Stream API Analytical Processing**: Employs `stream()`, `filter()`, `map()`, `collect(Collectors.groupingBy(...))` without any external analytics libraries.
* **Complete System Audit Trail**:
  * Logs `USER_LOGIN` events when users authenticate.
  * Logs `TASK_CREATED`, `TASK_UPDATED`, `TASK_COMPLETED`, and `TASK_DELETED`.
  * Logs `RESOURCE_ALLOCATED`, `RESOURCE_RELEASED`, and `RESOURCE_CONFLICT` blocks.
  * Records panel supports live keyword search and dropdown filtering by action type.
  * Multithreaded background export of audit trail to CSV via `SwingWorker`.

---

## 🚀 How to Run the Application

### Option 1: Double-Click Launcher (Windows)
Double-click **`run.bat`** in the project directory.

### Option 2: PowerShell
```powershell
.\run.ps1
```

### Option 3: Automated Verification Test Suite
To execute the headless test suite verifying all 7 subsystems:
```powershell
javac -encoding UTF-8 -cp 'bin;lib/*' -d 'bin' tools/ComprehensiveSystemTest.java
java --enable-native-access=ALL-UNNAMED -cp 'bin;lib/*' tools.ComprehensiveSystemTest
```

---

## 👥 Initial User Profiles (Role-Based Access Control)

| Username | Full Name | Role | Access Level |
|---|---|---|---|
| `admin` | Dr. Kumar | **ADMIN** | Full administrative access, user & resource management, clear audit logs |
| `younus_mgr` | Younus | **MANAGER** | Task and resource management, priority dispatch, conflict diagnostics |
| `purna_member` | Purna | **MEMBER** | Contributor access, task status & progress updates |
