package com.smartflow.gui;

import com.smartflow.model.ConflictReport;
import com.smartflow.model.User;
import com.smartflow.service.TaskService;

import javax.swing.*;
import java.awt.*;

/**
 * Diagnostic dialog for system-wide Resource Conflict Detection
 * and reliable task allocation reconciliation.
 */
public class ConflictScannerDialog extends JDialog {
    private final TaskService taskService;
    private final User currentUser;
    private final Runnable onDataChanged;

    private DefaultListModel<String> resourceConflictListModel;
    private DefaultListModel<String> workloadConflictListModel;
    private DefaultListModel<String> deadlineConflictListModel;
    private JLabel summaryLabel;

    public ConflictScannerDialog(Frame owner, TaskService taskService, User currentUser, Runnable onDataChanged) {
        super(owner, "Resource Conflict Detection & Allocation Diagnostics", true);
        this.taskService = taskService;
        this.currentUser = currentUser;
        this.onDataChanged = onDataChanged;

        setSize(680, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        initUI();
        runDiagnosticScan();
    }

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(Theme.BG_LIGHT);
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(8, 8));
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("System-Wide Conflict Scanner");
        title.setFont(Theme.FONT_HEADER);
        title.setForeground(Theme.TEXT_MAIN);

        summaryLabel = new JLabel("Scanning for capacity collisions and workload bottlenecks...");
        summaryLabel.setFont(Theme.FONT_REGULAR);
        summaryLabel.setForeground(Theme.TEXT_MUTED);

        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(summaryLabel, BorderLayout.CENTER);
        content.add(headerPanel, BorderLayout.NORTH);

        // Center: Tabs for different conflict categories
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_BOLD);

        // 1. Resource Conflicts Tab
        resourceConflictListModel = new DefaultListModel<>();
        JList<String> resourceList = new JList<>(resourceConflictListModel);
        resourceList.setFont(Theme.FONT_REGULAR);
        resourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabs.addTab("Resource Capacity Collisions", new JScrollPane(resourceList));

        // 2. Member Workload Conflicts Tab
        workloadConflictListModel = new DefaultListModel<>();
        JList<String> workloadList = new JList<>(workloadConflictListModel);
        workloadList.setFont(Theme.FONT_REGULAR);
        tabs.addTab("Member Workload Bottlenecks", new JScrollPane(workloadList));

        // 3. Deadline Conflicts Tab
        deadlineConflictListModel = new DefaultListModel<>();
        JList<String> deadlineList = new JList<>(deadlineConflictListModel);
        deadlineList.setFont(Theme.FONT_REGULAR);
        tabs.addTab("Concurrent Deadline Collisions", new JScrollPane(deadlineList));

        content.add(tabs, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        footer.setOpaque(false);

        JButton rebalanceBtn = Theme.createButton("Reconcile Allocations", new Color(139, 92, 246), Color.WHITE);
        rebalanceBtn.setToolTipText("Recalculate and synchronize allocated units against active tasks");
        rebalanceBtn.addActionListener(e -> onReconcile());
        footer.add(rebalanceBtn);

        JButton rescanBtn = Theme.createButton("Rescan", Theme.BORDER, Theme.TEXT_MAIN);
        rescanBtn.addActionListener(e -> runDiagnosticScan());
        footer.add(rescanBtn);

        JButton closeBtn = Theme.createPrimaryButton("Close");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);

        content.add(footer, BorderLayout.SOUTH);
        add(content);
    }

    private void runDiagnosticScan() {
        try {
            ConflictReport report = taskService.scanAllConflicts();

            resourceConflictListModel.clear();
            if (report.getResourceConflicts().isEmpty()) {
                resourceConflictListModel.addElement("✓ No resource over-allocation detected. All allocations within safe capacity limits.");
            } else {
                for (String c : report.getResourceConflicts()) {
                    resourceConflictListModel.addElement("⚠ " + c);
                }
            }

            workloadConflictListModel.clear();
            if (report.getWorkloadConflicts().isEmpty()) {
                workloadConflictListModel.addElement("✓ Team member workloads are well balanced (all members have < 3 active tasks).");
            } else {
                for (String c : report.getWorkloadConflicts()) {
                    workloadConflictListModel.addElement("⚠ " + c);
                }
            }

            deadlineConflictListModel.clear();
            if (report.getDeadlineConflicts().isEmpty()) {
                deadlineConflictListModel.addElement("✓ No same-day multiple deadline collisions detected.");
            } else {
                for (String c : report.getDeadlineConflicts()) {
                    deadlineConflictListModel.addElement("⚠ " + c);
                }
            }

            if (report.hasAnyConflicts()) {
                summaryLabel.setText("Found " + report.getTotalConflictCount() + " potential conflict(s) requiring attention.");
                summaryLabel.setForeground(Theme.DANGER);
            } else {
                summaryLabel.setText("System diagnostic: 100% Conflict-Free. Optimal task allocation.");
                summaryLabel.setForeground(Theme.SUCCESS);
            }
        } catch (Exception ex) {
            summaryLabel.setText("Scan failed: " + ex.getMessage());
            summaryLabel.setForeground(Theme.DANGER);
        }
    }

    private void onReconcile() {
        try {
            int fixed = taskService.reconcileResourceAllocations(currentUser);
            if (fixed > 0) {
                JOptionPane.showMessageDialog(this,
                        "Reconciliation complete! Re-synchronized " + fixed + " resource allocation counter(s).",
                        "Allocations Synchronized", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "All resource counters are already perfectly in sync with active database tasks.",
                        "System Consistent", JOptionPane.INFORMATION_MESSAGE);
            }
            if (onDataChanged != null) {
                onDataChanged.run();
            }
            runDiagnosticScan();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Reconciliation failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
