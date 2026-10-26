package com.smartflow.gui;

import com.smartflow.exception.ResourceConflictException;
import com.smartflow.exception.ValidationException;
import com.smartflow.model.*;
import com.smartflow.service.ResourceService;
import com.smartflow.service.TaskService;
import com.smartflow.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Modal Dialog for adding/editing tasks with input validation,
 * live Resource Conflict Detection, and Workload Advisories.
 */
public class TaskDialog extends JDialog {
    private final TaskService taskService;
    private final ResourceService resourceService;
    private final User currentUser;
    private final Task existingTask;
    private final List<User> users;
    private List<Resource> allResources;
    private List<Task> allExistingTasks;
    private boolean saved = false;

    private JTextField titleField;
    private JTextArea descArea;
    private JComboBox<TaskPriority> priorityCombo;
    private JComboBox<TaskStatus> statusCombo;
    private JComboBox<UserItem> userCombo;
    private JLabel userWorkloadLabel;
    private JComboBox<ResourceItem> resourceCombo;
    private JLabel resourceStatusLabel;
    private JTextField dueDateField;
    private JSpinner hoursSpinner;
    private JSlider progressSlider;
    private JLabel progressLabel;

    public TaskDialog(Frame owner, TaskService taskService, ResourceService resourceService,
                      List<User> users, User currentUser, Task existingTask) {
        super(owner, existingTask == null ? "Create New Task" : "Edit Task #" + existingTask.getId(), true);
        this.taskService = taskService;
        this.resourceService = resourceService;
        this.users = users;
        this.currentUser = currentUser;
        this.existingTask = existingTask;

        try {
            this.allResources = resourceService.getAllResources();
            this.allExistingTasks = taskService.getAllTasks();
        } catch (Exception e) {
            this.allResources = List.of();
            this.allExistingTasks = List.of();
        }

        initUI();
        if (existingTask != null) {
            populateFields(existingTask);
        } else {
            updateResourceConflictAdvisory();
            updateUserWorkloadAdvisory();
        }
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Title
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Task Title * :"), gbc);
        gbc.gridx = 1;
        titleField = new JTextField(26);
        formPanel.add(titleField, gbc);

        // Description
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        descArea = new JTextArea(3, 26);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(descArea), gbc);

        // Priority
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Priority:"), gbc);
        gbc.gridx = 1;
        priorityCombo = new JComboBox<>(TaskPriority.values());
        formPanel.add(priorityCombo, gbc);

        // Status
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        statusCombo = new JComboBox<>(TaskStatus.values());
        statusCombo.addActionListener(e -> updateResourceConflictAdvisory());
        formPanel.add(statusCombo, gbc);

        // Assignee
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Assignee:"), gbc);
        gbc.gridx = 1;
        userCombo = new JComboBox<>();
        userCombo.addItem(new UserItem(0, "-- Unassigned --"));
        for (User u : users) {
            userCombo.addItem(new UserItem(u.getId(), u.getFullName() + " (" + u.getRole() + ")"));
        }
        userCombo.addActionListener(e -> updateUserWorkloadAdvisory());
        formPanel.add(userCombo, gbc);

        // User Workload Advisory Label
        row++;
        gbc.gridx = 1; gbc.gridy = row;
        userWorkloadLabel = new JLabel(" ");
        userWorkloadLabel.setFont(Theme.FONT_SMALL);
        formPanel.add(userWorkloadLabel, gbc);

        // Resource Allocation
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Resource:"), gbc);
        gbc.gridx = 1;
        resourceCombo = new JComboBox<>();
        resourceCombo.addItem(new ResourceItem(0, "-- None --"));
        for (Resource r : allResources) {
            resourceCombo.addItem(new ResourceItem(r.getId(), r.getName() + " (" + r.getAvailableQuantity() + "/" + r.getTotalCapacity() + " avail)"));
        }
        resourceCombo.addActionListener(e -> updateResourceConflictAdvisory());
        formPanel.add(resourceCombo, gbc);

        // Resource Conflict Detection Status Label
        row++;
        gbc.gridx = 1; gbc.gridy = row;
        resourceStatusLabel = new JLabel(" ");
        resourceStatusLabel.setFont(Theme.FONT_SMALL);
        formPanel.add(resourceStatusLabel, gbc);

        // Due Date
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Due Date (YYYY-MM-DD) * :"), gbc);
        gbc.gridx = 1;
        dueDateField = new JTextField(DateUtil.format(LocalDate.now().plusDays(7)));
        formPanel.add(dueDateField, gbc);

        // Estimated Hours
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Estimated Hours:"), gbc);
        gbc.gridx = 1;
        hoursSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 500, 1));
        formPanel.add(hoursSpinner, gbc);

        // Progress Slider
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        progressLabel = new JLabel("Progress: 0%");
        formPanel.add(progressLabel, gbc);
        gbc.gridx = 1;
        progressSlider = new JSlider(0, 100, 0);
        progressSlider.setMajorTickSpacing(25);
        progressSlider.setPaintTicks(true);
        progressSlider.addChangeListener(e -> {
            int val = progressSlider.getValue();
            progressLabel.setText("Progress: " + val + "%");
            if (val == 100 && statusCombo.getSelectedItem() != TaskStatus.COMPLETED) {
                statusCombo.setSelectedItem(TaskStatus.COMPLETED);
            }
        });
        formPanel.add(progressSlider, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Buttons Bar
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonBar.setBackground(Theme.BG_LIGHT);

        JButton cancelBtn = Theme.createButton("Cancel", Theme.BORDER, Theme.TEXT_MAIN);
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = Theme.createPrimaryButton(existingTask == null ? "Create Task" : "Save Changes");
        saveBtn.addActionListener(e -> onSave());

        buttonBar.add(cancelBtn);
        buttonBar.add(saveBtn);
        add(buttonBar, BorderLayout.SOUTH);
    }

    private void updateResourceConflictAdvisory() {
        if (resourceCombo == null || resourceStatusLabel == null) return;
        ResourceItem item = (ResourceItem) resourceCombo.getSelectedItem();
        if (item == null || item.id == 0) {
            resourceStatusLabel.setText("✓ No resource reserved for this task.");
            resourceStatusLabel.setForeground(Theme.TEXT_MUTED);
            return;
        }

        Resource res = allResources.stream().filter(r -> r.getId() == item.id).findFirst().orElse(null);
        if (res == null) return;

        TaskStatus status = (TaskStatus) statusCombo.getSelectedItem();
        boolean isCompleted = status == TaskStatus.COMPLETED;

        if (isCompleted) {
            resourceStatusLabel.setText("✓ Completed task does not occupy active capacity.");
            resourceStatusLabel.setForeground(Theme.SUCCESS);
            return;
        }

        // Check if existing task already held this resource
        boolean alreadyHeld = existingTask != null && existingTask.getResourceId() == res.getId() && !existingTask.isCompleted();
        int available = res.getAvailableQuantity() + (alreadyHeld ? 1 : 0);

        if (available <= 0) {
            // CONFLICT DETECTED!
            List<Resource> alternatives = allResources.stream()
                    .filter(r -> r.getId() != res.getId() && r.getType() == res.getType() && r.getAvailableQuantity() > 0)
                    .collect(Collectors.toList());

            String altText = alternatives.isEmpty() ? "" : " (Alt: " + alternatives.get(0).getName() + " has " + alternatives.get(0).getAvailableQuantity() + " free)";
            resourceStatusLabel.setText("⚠ CONFLICT: Fully booked (0 available)!" + altText);
            resourceStatusLabel.setForeground(Theme.DANGER);
        } else {
            resourceStatusLabel.setText("✓ Resource Available (" + available + " unit(s) free)");
            resourceStatusLabel.setForeground(Theme.SUCCESS);
        }
    }

    private void updateUserWorkloadAdvisory() {
        if (userCombo == null || userWorkloadLabel == null) return;
        UserItem item = (UserItem) userCombo.getSelectedItem();
        if (item == null || item.id == 0) {
            userWorkloadLabel.setText("✓ Task is unassigned.");
            userWorkloadLabel.setForeground(Theme.TEXT_MUTED);
            return;
        }

        int currTaskId = existingTask != null ? existingTask.getId() : 0;
        long activeCount = allExistingTasks.stream()
                .filter(t -> t.getAssignedUserId() == item.id && !t.isCompleted() && t.getId() != currTaskId)
                .count();

        if (activeCount >= 3) {
            userWorkloadLabel.setText("⚠ High Workload: Assignee currently has " + activeCount + " active tasks.");
            userWorkloadLabel.setForeground(Theme.DANGER);
        } else if (activeCount > 0) {
            userWorkloadLabel.setText("✓ Assignee has " + activeCount + " active task(s). Safe to allocate.");
            userWorkloadLabel.setForeground(Theme.SUCCESS);
        } else {
            userWorkloadLabel.setText("✓ Assignee has 0 active tasks (Optimal availability).");
            userWorkloadLabel.setForeground(Theme.SUCCESS);
        }
    }

    private void populateFields(Task t) {
        titleField.setText(t.getTitle());
        descArea.setText(t.getDescription());
        priorityCombo.setSelectedItem(t.getPriority());
        statusCombo.setSelectedItem(t.getStatus());

        for (int i = 0; i < userCombo.getItemCount(); i++) {
            if (userCombo.getItemAt(i).id == t.getAssignedUserId()) {
                userCombo.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < resourceCombo.getItemCount(); i++) {
            if (resourceCombo.getItemAt(i).id == t.getResourceId()) {
                resourceCombo.setSelectedIndex(i);
                break;
            }
        }

        if (t.getDueDate() != null) {
            dueDateField.setText(DateUtil.format(t.getDueDate()));
        }
        hoursSpinner.setValue(t.getEstimatedHours());
        progressSlider.setValue(t.getProgress());
        progressLabel.setText("Progress: " + t.getProgress() + "%");

        updateResourceConflictAdvisory();
        updateUserWorkloadAdvisory();
    }

    private void onSave() {
        try {
            LocalDate due = DateUtil.parse(dueDateField.getText());
            if (due == null) {
                throw new ValidationException("Due Date", "Please enter a valid date in YYYY-MM-DD format.");
            }

            Task task = existingTask != null ? existingTask : new Task();
            task.setTitle(titleField.getText());
            task.setDescription(descArea.getText());
            task.setPriority((TaskPriority) priorityCombo.getSelectedItem());
            task.setStatus((TaskStatus) statusCombo.getSelectedItem());

            UserItem uItem = (UserItem) userCombo.getSelectedItem();
            task.setAssignedUserId(uItem != null ? uItem.id : 0);

            ResourceItem rItem = (ResourceItem) resourceCombo.getSelectedItem();
            task.setResourceId(rItem != null ? rItem.id : 0);

            task.setDueDate(due);
            task.setEstimatedHours((Integer) hoursSpinner.getValue());
            task.setProgress(progressSlider.getValue());

            if (existingTask == null) {
                taskService.createTask(task, currentUser);
            } else {
                taskService.updateTask(task, currentUser);
            }

            saved = true;
            dispose();
        } catch (ValidationException | ResourceConflictException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Resource Conflict / Validation Advisory", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save task: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private static class UserItem {
        final int id;
        final String name;
        UserItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    private static class ResourceItem {
        final int id;
        final String name;
        ResourceItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}
