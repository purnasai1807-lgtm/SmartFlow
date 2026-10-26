package com.smartflow.thread;

import com.smartflow.model.Task;

import javax.swing.SwingWorker;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Asynchronous Background Worker.
 * Exports system records/tasks to CSV on a separate thread, keeping the GUI responsive.
 */
public class AsyncReportExporter extends SwingWorker<File, Integer> {
    private final List<Task> tasks;
    private final File targetFile;
    private final Consumer<String> statusListener;

    public AsyncReportExporter(List<Task> tasks, File targetFile, Consumer<String> statusListener) {
        this.tasks = tasks;
        this.targetFile = targetFile;
        this.statusListener = statusListener;
    }

    @Override
    protected File doInBackground() throws Exception {
        if (statusListener != null) {
            statusListener.accept("Preparing export file...");
        }
        Thread.sleep(300); // Simulate processing work

        try (PrintWriter writer = new PrintWriter(new FileWriter(targetFile))) {
            writer.println("Task ID,Title,Priority,Status,Assignee,Resource,Due Date,Estimated Hours,Progress %");
            int total = tasks.size();
            for (int i = 0; i < total; i++) {
                Task t = tasks.get(i);
                writer.printf("%d,\"%s\",%s,%s,\"%s\",\"%s\",%s,%d,%d%%%n",
                        t.getId(),
                        escapeCsv(t.getTitle()),
                        t.getPriority(),
                        t.getStatus(),
                        t.getAssignedUserName() != null ? escapeCsv(t.getAssignedUserName()) : "Unassigned",
                        t.getResourceName() != null ? escapeCsv(t.getResourceName()) : "None",
                        t.getDueDate() != null ? t.getDueDate().toString() : "N/A",
                        t.getEstimatedHours(),
                        t.getProgress()
                );
                setProgress((int) (((i + 1) / (double) total) * 100));
            }
        }
        return targetFile;
    }

    private String escapeCsv(String str) {
        return str.replace("\"", "\"\"");
    }
}
