package com.smartflow.gui;

import com.smartflow.dao.RecordDao;
import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.SystemRecord;
import com.smartflow.model.User;
import com.smartflow.service.TaskService;
import com.smartflow.thread.AsyncAuditExporter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * System Records / Audit Log panel with Live Filtering, Action Badges,
 * and Multithreaded Async CSV Audit Trail Export.
 */
public class RecordsPanel extends JPanel {
    private final RecordDao recordDao;
    private final TaskService taskService;
    private final User currentUser;

    private JTable recordsTable;
    private DefaultTableModel tableModel;
    private JProgressBar exportProgressBar;
    private JLabel statusLabel;
    private JLabel summaryStatsLabel;

    private JTextField searchField;
    private JComboBox<String> actionFilterCombo;

    private List<SystemRecord> allRecords = new ArrayList<>();
    private List<SystemRecord> filteredRecords = new ArrayList<>();

    public RecordsPanel(RecordDao recordDao, TaskService taskService, User currentUser) {
        this.recordDao = recordDao;
        this.taskService = taskService;
        this.currentUser = currentUser;

        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        initToolbar();
        initTable();
    }

    private void initToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 8));
        bar.setOpaque(false);

        // Top line: Title & Summary
        JPanel headerLine = new JPanel(new BorderLayout());
        headerLine.setOpaque(false);

        JLabel label = new JLabel("System Audit Trail & Operational Records");
        label.setFont(Theme.FONT_HEADER);
        label.setForeground(Theme.TEXT_MAIN);

        summaryStatsLabel = new JLabel("Loading audit records...");
        summaryStatsLabel.setFont(Theme.FONT_SMALL);
        summaryStatsLabel.setForeground(Theme.TEXT_MUTED);

        headerLine.add(label, BorderLayout.WEST);
        headerLine.add(summaryStatsLabel, BorderLayout.EAST);
        bar.add(headerLine, BorderLayout.NORTH);

        // Bottom line: Search, Filter, & Actions
        JPanel controlsLine = new JPanel(new BorderLayout(8, 8));
        controlsLine.setOpaque(false);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.setOpaque(false);

        filters.add(new JLabel("Search:"));
        searchField = new JTextField(12);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
        filters.add(searchField);

        filters.add(new JLabel("Action:"));
        actionFilterCombo = new JComboBox<>(new String[]{
                "ALL",
                "USER_LOGIN",
                "TASK_CREATED",
                "TASK_UPDATED",
                "TASK_COMPLETED",
                "TASK_DELETED",
                "RESOURCE_ALLOCATED",
                "RESOURCE_RELEASED",
                "RESOURCE_CONFLICT",
                "WORKLOAD_WARNING",
                "RESOURCE_CREATED",
                "RESOURCE_UPDATED",
                "RESOURCE_DELETED",
                "ALLOCATION_RECONCILED"
        });
        actionFilterCombo.addActionListener(e -> applyFilters());
        filters.add(actionFilterCombo);

        controlsLine.add(filters, BorderLayout.WEST);

        // Right Action Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);

        statusLabel = new JLabel("");
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_MUTED);
        actions.add(statusLabel);

        exportProgressBar = new JProgressBar(0, 100);
        exportProgressBar.setPreferredSize(new Dimension(100, 20));
        exportProgressBar.setVisible(false);
        actions.add(exportProgressBar);

        JButton refreshBtn = Theme.createButton("Refresh Logs", Theme.BORDER, Theme.TEXT_MAIN);
        refreshBtn.addActionListener(e -> reloadData());
        actions.add(refreshBtn);

        JButton exportBtn = Theme.createPrimaryButton("Export Audit CSV (Async)");
        exportBtn.setToolTipText("Export the complete verified system audit trail to CSV using background SwingWorker");
        exportBtn.addActionListener(e -> onExportCsv());
        actions.add(exportBtn);

        if (currentUser != null && currentUser.canClearAuditLogs()) {
            JButton clearBtn = Theme.createDangerButton("Clear Logs");
            clearBtn.setToolTipText("Administrative action: permanently clear historical audit records");
            clearBtn.addActionListener(e -> onClearLogs());
            actions.add(clearBtn);
        }

        controlsLine.add(actions, BorderLayout.EAST);
        bar.add(controlsLine, BorderLayout.SOUTH);

        add(bar, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] cols = {"Log ID", "Timestamp", "Action", "Entity", "Entity ID", "Performed By", "Details"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        recordsTable = new JTable(tableModel);
        recordsTable.setRowHeight(26);
        recordsTable.setFont(Theme.FONT_REGULAR);
        recordsTable.getTableHeader().setFont(Theme.FONT_BOLD);
        recordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        recordsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        recordsTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        recordsTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        recordsTable.getColumnModel().getColumn(6).setPreferredWidth(340);

        // Custom badge styling for Action column
        recordsTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setFont(Theme.FONT_BOLD);
                String action = value != null ? value.toString() : "";
                if (action.contains("CONFLICT") || action.contains("WARNING")) {
                    l.setForeground(Theme.DANGER);
                } else if (action.contains("COMPLETED") || action.contains("RELEASED")) {
                    l.setForeground(Theme.SUCCESS);
                } else if (action.contains("LOGIN")) {
                    l.setForeground(new Color(139, 92, 246));
                } else if (action.contains("RECONCILED")) {
                    l.setForeground(new Color(14, 165, 233));
                } else {
                    l.setForeground(Theme.PRIMARY);
                }
                return l;
            }
        });

        JScrollPane scroll = new JScrollPane(recordsTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(scroll, BorderLayout.CENTER);
    }

    public void reloadData() {
        try {
            allRecords = recordDao.findAll();
            applyFilters();
            updateSummaryStats();
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Failed to load audit records: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilters() {
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String actionFilter = actionFilterCombo != null ? (String) actionFilterCombo.getSelectedItem() : "ALL";

        filteredRecords = allRecords.stream()
                .filter(r -> {
                    // Action filter
                    if (actionFilter != null && !actionFilter.equals("ALL")) {
                        if (!r.getAction().equalsIgnoreCase(actionFilter)) {
                            return false;
                        }
                    }
                    // Search query filter
                    if (!query.isEmpty()) {
                        boolean matchAction = r.getAction().toLowerCase().contains(query);
                        boolean matchActor = r.getPerformedBy().toLowerCase().contains(query);
                        boolean matchEntity = r.getEntityType().toLowerCase().contains(query);
                        boolean matchDetails = r.getDetails() != null && r.getDetails().toLowerCase().contains(query);
                        return matchAction || matchActor || matchEntity || matchDetails;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        tableModel.setRowCount(0);
        for (SystemRecord r : filteredRecords) {
            tableModel.addRow(new Object[]{
                    r.getId(),
                    r.getFormattedTimestamp(),
                    r.getAction(),
                    r.getEntityType(),
                    r.getEntityId(),
                    r.getPerformedBy(),
                    r.getDetails()
            });
        }
    }

    private void updateSummaryStats() {
        long conflictCount = allRecords.stream()
                .filter(r -> r.getAction().contains("CONFLICT") || r.getAction().contains("WARNING"))
                .count();

        long uniqueUsers = allRecords.stream()
                .map(SystemRecord::getPerformedBy)
                .distinct()
                .count();

        summaryStatsLabel.setText("Total Audit Events: " + allRecords.size()
                + "  |  Conflicts Prevented: " + conflictCount
                + "  |  Active Actors: " + uniqueUsers);
    }

    private void onExportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("smartflow_audit_trail.csv"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            List<SystemRecord> toExport = filteredRecords.isEmpty() ? allRecords : filteredRecords;

            exportProgressBar.setVisible(true);
            exportProgressBar.setValue(0);
            statusLabel.setText("Exporting audit trail...");

            // Background SwingWorker execution
            AsyncAuditExporter worker = new AsyncAuditExporter(toExport, target, msg -> SwingUtilities.invokeLater(() -> statusLabel.setText(msg)));
            worker.addPropertyChangeListener(evt -> {
                if ("progress".equals(evt.getPropertyName())) {
                    exportProgressBar.setValue((Integer) evt.getNewValue());
                }
            });

            new Thread(() -> {
                try {
                    worker.execute();
                    worker.get();
                    SwingUtilities.invokeLater(() -> {
                        exportProgressBar.setVisible(false);
                        statusLabel.setText("Audit export completed!");
                        JOptionPane.showMessageDialog(this, "Audit trail exported successfully (" + toExport.size() + " records):\n" + target.getAbsolutePath(),
                                "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        exportProgressBar.setVisible(false);
                        statusLabel.setText("Export failed.");
                        JOptionPane.showMessageDialog(this, "Export error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }
    }

    private void onClearLogs() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to permanently clear the audit trail? (Admin privilege required)",
                "Confirm Clear Audit Trail", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                recordDao.clearAll();
                recordDao.log(new SystemRecord("AUDIT_CLEARED", "SYSTEM", 0,
                        "Historical audit records cleared by administrator.", currentUser.getUsername()));
                reloadData();
            } catch (DatabaseOperationException e) {
                JOptionPane.showMessageDialog(this, "Failed to clear logs: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
