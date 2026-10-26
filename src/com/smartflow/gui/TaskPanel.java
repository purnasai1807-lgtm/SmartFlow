package com.smartflow.gui;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.*;
import com.smartflow.service.ResourceService;
import com.smartflow.service.TaskService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Interactive Task Management View with Event Handling, Live Filtering & Table Rendering.
 */
public class TaskPanel extends JPanel {
    private final TaskService taskService;
    private final ResourceService resourceService;
    private final List<User> users;
    private final User currentUser;
    private final Runnable onDataChanged;

    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;
    private JComboBox<String> priorityFilterCombo;

    private List<Task> currentList;

    public TaskPanel(TaskService taskService, ResourceService resourceService, List<User> users,
                     User currentUser, Runnable onDataChanged) {
        this.taskService = taskService;
        this.resourceService = resourceService;
        this.users = users;
        this.currentUser = currentUser;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        initToolbar();
        initTable();
    }

    private void initToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 8));
        bar.setBackground(Theme.BG_LIGHT);

        // Left controls: Search & Filters
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterPanel.setOpaque(false);

        filterPanel.add(new JLabel("Search:"));
        searchField = new JTextField(12);
        // Live Search Event Listener (DocumentListener)
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
        filterPanel.add(searchField);

        filterPanel.add(new JLabel("Status:"));
        statusFilterCombo = new JComboBox<>(new String[]{"ALL", "BACKLOG", "IN_PROGRESS", "REVIEW", "COMPLETED"});
        statusFilterCombo.addActionListener(e -> applyFilters());
        filterPanel.add(statusFilterCombo);

        filterPanel.add(new JLabel("Priority:"));
        priorityFilterCombo = new JComboBox<>(new String[]{"ALL", "LOW", "MEDIUM", "HIGH", "CRITICAL"});
        priorityFilterCombo.addActionListener(e -> applyFilters());
        filterPanel.add(priorityFilterCombo);

        // Right controls: Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actionPanel.setOpaque(false);

        JButton pqBtn = Theme.createButton("Priority Dispatch", new Color(139, 92, 246), Color.WHITE);
        pqBtn.setToolTipText("View next urgent tasks sorted by PriorityQueue");
        pqBtn.addActionListener(e -> showPriorityQueueDialog());
        actionPanel.add(pqBtn);

        JButton conflictBtn = Theme.createButton("Conflict Scanner", new Color(245, 158, 11), Color.WHITE);
        conflictBtn.setToolTipText("Scan for resource capacity collisions and team workload bottlenecks");
        conflictBtn.addActionListener(e -> openConflictScanner());
        actionPanel.add(conflictBtn);

        JButton newBtn = Theme.createPrimaryButton("+ New Task");
        newBtn.addActionListener(e -> openTaskDialog(null));
        actionPanel.add(newBtn);

        JButton editBtn = Theme.createButton("Edit", Theme.BORDER, Theme.TEXT_MAIN);
        editBtn.addActionListener(e -> onEditSelected());
        actionPanel.add(editBtn);

        JButton deleteBtn = Theme.createDangerButton("Delete");
        // Role-based authorization
        deleteBtn.setEnabled(currentUser.canDeleteTasks());
        deleteBtn.setToolTipText(currentUser.canDeleteTasks() ? "Delete selected task" : "Requires Manager or Admin permissions");
        deleteBtn.addActionListener(e -> onDeleteSelected());
        actionPanel.add(deleteBtn);

        bar.add(filterPanel, BorderLayout.WEST);
        bar.add(actionPanel, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] columns = {"ID", "Title", "Priority", "Status", "Assignee", "Resource", "Due Date", "Progress"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(28);
        taskTable.setFont(Theme.FONT_REGULAR);
        taskTable.getTableHeader().setFont(Theme.FONT_BOLD);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom column styling
        taskTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        taskTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        taskTable.getColumnModel().getColumn(7).setCellRenderer(new ProgressBarRenderer());

        // Double-click event to edit
        taskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && taskTable.getSelectedRow() != -1) {
                    onEditSelected();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(taskTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void reloadData(List<Task> tasks) {
        this.currentList = tasks;
        applyFilters();
    }

    private void applyFilters() {
        if (currentList == null) return;

        String query = searchField.getText().trim().toLowerCase();
        String statusFilter = (String) statusFilterCombo.getSelectedItem();
        String priorityFilter = (String) priorityFilterCombo.getSelectedItem();

        // Stream API filtering
        List<Task> filtered = currentList.stream()
                .filter(t -> query.isEmpty() || t.getTitle().toLowerCase().contains(query)
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(query)))
                .filter(t -> "ALL".equals(statusFilter) || t.getStatus().name().equals(statusFilter))
                .filter(t -> "ALL".equals(priorityFilter) || t.getPriority().name().equals(priorityFilter))
                .collect(Collectors.toList());

        tableModel.setRowCount(0);
        for (Task t : filtered) {
            tableModel.addRow(new Object[]{
                    t.getId(),
                    t.getTitle(),
                    t.getPriority().getDisplayName(),
                    t.getStatus().getDisplayName(),
                    t.getAssignedUserName() != null ? t.getAssignedUserName() : "Unassigned",
                    t.getResourceName() != null ? t.getResourceName() : "None",
                    t.getDueDate() != null ? t.getDueDate().toString() : "N/A",
                    t.getProgress()
            });
        }
    }

    private Task getSelectedTask() {
        int row = taskTable.getSelectedRow();
        if (row == -1) return null;
        int id = (int) tableModel.getValueAt(row, 0);
        return currentList.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    private void onEditSelected() {
        Task t = getSelectedTask();
        if (t != null) {
            openTaskDialog(t);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a task from the table to edit.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onDeleteSelected() {
        Task t = getSelectedTask();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Please select a task to delete.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete task #" + t.getId() + " (" + t.getTitle() + ")?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                taskService.deleteTask(t.getId(), currentUser);
                if (onDataChanged != null) onDataChanged.run();
            } catch (DatabaseOperationException ex) {
                JOptionPane.showMessageDialog(this, "Failed to delete task: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openTaskDialog(Task task) {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        TaskDialog dlg = new TaskDialog(parent, taskService, resourceService, users, currentUser, task);
        dlg.setVisible(true);
        if (dlg.isSaved() && onDataChanged != null) {
            onDataChanged.run();
        }
    }

    private void openConflictScanner() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        ConflictScannerDialog dlg = new ConflictScannerDialog(parent, taskService, currentUser, onDataChanged);
        dlg.setVisible(true);
    }

    private void showPriorityQueueDialog() {
        try {
            // Retrieving elements from PriorityQueue
            PriorityQueue<Task> pq = taskService.getPriorityTaskQueue();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-6s | %-10s | %-12s | %s%n", "ID", "Priority", "Due Date", "Title"));
            sb.append("-".repeat(55)).append("\n");

            if (pq.isEmpty()) {
                sb.append("No active pending tasks in the priority queue.\n");
            } else {
                int rank = 1;
                while (!pq.isEmpty()) {
                    Task t = pq.poll();
                    sb.append(String.format("#%-4d | %-10s | %-12s | %s%n",
                            t.getId(), t.getPriority(), t.getDueDate(), t.getTitle()));
                    rank++;
                }
            }

            JTextArea text = new JTextArea(sb.toString(), 14, 50);
            text.setFont(new Font("Consolas", Font.PLAIN, 12));
            text.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(text), "Priority Task Dispatch (Collection PriorityQueue)", JOptionPane.INFORMATION_MESSAGE);
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, "Error querying priority queue: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class ProgressBarRenderer extends DefaultTableCellRenderer {
        private final JProgressBar bar = new JProgressBar(0, 100);

        ProgressBarRenderer() {
            bar.setStringPainted(true);
            bar.setFont(Theme.FONT_SMALL);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            int val = value instanceof Number ? ((Number) value).intValue() : 0;
            bar.setValue(val);
            bar.setForeground(val == 100 ? Theme.SUCCESS : Theme.PRIMARY);
            return bar;
        }
    }
}
