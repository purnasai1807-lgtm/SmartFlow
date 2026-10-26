package com.smartflow.thread;

import com.smartflow.model.Task;
import com.smartflow.service.TaskService;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.function.Consumer;

/**
 * Background Multithreading Daemon.
 * Continuously monitors task deadlines and alerts the user interface without blocking it.
 */
public class DeadlineMonitorDaemon extends Thread {
    private final TaskService taskService;
    private final Consumer<List<Task>> alertCallback;
    private volatile boolean running = true;
    private final long checkIntervalMs;

    public DeadlineMonitorDaemon(TaskService taskService, Consumer<List<Task>> alertCallback, long checkIntervalMs) {
        this.taskService = taskService;
        this.alertCallback = alertCallback;
        this.checkIntervalMs = checkIntervalMs;
        setName("SmartFlow-Deadline-Monitor");
        setDaemon(true); // Background daemon thread
    }

    @Override
    public void run() {
        System.out.println("[Thread] " + getName() + " started in background.");
        while (running) {
            try {
                List<Task> allTasks = taskService.getAllTasks();
                List<Task> urgentTasks = allTasks.stream()
                        .filter(t -> t.isOverdue() || t.isDueSoon())
                        .toList();

                if (!urgentTasks.isEmpty() && alertCallback != null) {
                    // Safely dispatch update to Swing Event Dispatch Thread (EDT)
                    SwingUtilities.invokeLater(() -> alertCallback.accept(urgentTasks));
                }

                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                System.out.println("[Thread] " + getName() + " interrupted, terminating cleanly.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[Thread] Error in background monitoring: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    public void stopMonitoring() {
        this.running = false;
        this.interrupt();
    }
}
