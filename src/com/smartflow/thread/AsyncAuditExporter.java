package com.smartflow.thread;

import com.smartflow.model.SystemRecord;

import javax.swing.SwingWorker;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Asynchronous Background Worker.
 * Exports real system audit trail records to CSV on a separate thread,
 * ensuring the Swing GUI remains completely responsive.
 */
public class AsyncAuditExporter extends SwingWorker<File, Integer> {
    private final List<SystemRecord> records;
    private final File targetFile;
    private final Consumer<String> statusListener;

    public AsyncAuditExporter(List<SystemRecord> records, File targetFile, Consumer<String> statusListener) {
        this.records = records;
        this.targetFile = targetFile;
        this.statusListener = statusListener;
    }

    @Override
    protected File doInBackground() throws Exception {
        if (statusListener != null) {
            statusListener.accept("Preparing audit trail export file...");
        }
        Thread.sleep(250); // Background processing delay

        try (PrintWriter writer = new PrintWriter(new FileWriter(targetFile))) {
            // Write standard CSV header
            writer.println("Log ID,Timestamp,Action,Entity Type,Entity ID,Performed By,Details");
            int total = records.size();
            for (int i = 0; i < total; i++) {
                SystemRecord r = records.get(i);
                writer.printf("%d,\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\"%n",
                        r.getId(),
                        escapeCsv(r.getFormattedTimestamp()),
                        escapeCsv(r.getAction()),
                        escapeCsv(r.getEntityType()),
                        r.getEntityId(),
                        escapeCsv(r.getPerformedBy()),
                        escapeCsv(r.getDetails())
                );
                setProgress((int) (((i + 1) / (double) total) * 100));
            }
        }
        return targetFile;
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }
}
