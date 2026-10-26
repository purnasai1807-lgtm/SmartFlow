package com.smartflow.gui;

import com.smartflow.dao.RecordDao;
import com.smartflow.dao.UserDao;
import com.smartflow.model.Resource;
import com.smartflow.model.Task;
import com.smartflow.model.User;
import com.smartflow.service.AnalyticsService;
import com.smartflow.service.ResourceService;
import com.smartflow.service.TaskService;
import com.smartflow.thread.DeadlineMonitorDaemon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main Application Window incorporating Swing tabs, event handling,
 * real-time background thread integration, and custom graphics.
 */
public class MainFrame extends JFrame {
    private final User currentUser;
    private final TaskService taskService;
    private final ResourceService resourceService;
    private final AnalyticsService analyticsService;
    private final UserDao userDao;
    private final RecordDao recordDao;
    private final Runnable onSwitchUser;

    private DashboardOverviewPanel dashboardTab;
    private TaskPanel tasksTab;
    private ResourcePanel resourcesTab;
    private RecordsPanel recordsTab;

    private JLabel clockLabel;
    private JLabel statusLabel;
    private DeadlineMonitorDaemon deadlineDaemon;

    public MainFrame(User currentUser, TaskService taskService, ResourceService resourceService,
                     AnalyticsService analyticsService, UserDao userDao, RecordDao recordDao,
                     Runnable onSwitchUser) {
        super("SmartFlow - Enterprise Task & Resource Management Suite");
        this.currentUser = currentUser;
        this.taskService = taskService;
        this.resourceService = resourceService;
        this.analyticsService = analyticsService;
        this.userDao = userDao;
        this.recordDao = recordDao;
        this.onSwitchUser = onSwitchUser;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);

        initUI();
        startBackgroundServices();
        refreshAllData();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (deadlineDaemon != null) {
                    deadlineDaemon.stopMonitoring();
                }
            }
        });
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_LIGHT);

        // Header Panel
        root.add(createHeaderPanel(), BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(Theme.FONT_BOLD);
        tabbedPane.setBackground(Theme.BG_LIGHT);

        // 1. Dashboard Tab
        dashboardTab = new DashboardOverviewPanel(analyticsService);
        tabbedPane.addTab("  Dashboard & Analytics  ", dashboardTab);

        // 2. Tasks Tab
        List<User> allUsers = List.of();
        try {
            allUsers = userDao.findAll();
        } catch (Exception ignored) {}

        tasksTab = new TaskPanel(taskService, resourceService, allUsers, currentUser, this::refreshAllData);
        tabbedPane.addTab("  Task Management  ", tasksTab);

        // 3. Resources Tab
        resourcesTab = new ResourcePanel(resourceService, currentUser, this::refreshAllData);
        tabbedPane.addTab("  Resource Allocation  ", resourcesTab);

        // 4. Audit Records Tab
        recordsTab = new RecordsPanel(recordDao, taskService, currentUser);
        tabbedPane.addTab("  Audit Records  ", recordsTab);

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == recordsTab) {
                recordsTab.reloadData();
            }
        });

        root.add(tabbedPane, BorderLayout.CENTER);

        // Footer Status Bar
        root.add(createFooterPanel(), BorderLayout.SOUTH);

        add(root);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setBackground(Theme.BG_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // Brand Title
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);

        JLabel logo = new JLabel("SMARTFLOW");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(Color.WHITE);

        JLabel tag = new JLabel("|  Enterprise Resource & Task Suite");
        tag.setFont(Theme.FONT_REGULAR);
        tag.setForeground(new Color(148, 163, 184));

        brand.add(logo);
        brand.add(tag);
        header.add(brand, BorderLayout.WEST);

        // User info & Clock
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);

        clockLabel = new JLabel();
        clockLabel.setFont(Theme.FONT_REGULAR);
        clockLabel.setForeground(new Color(203, 213, 225));
        right.add(clockLabel);

        // Real-time clock timer
        Timer clockTimer = new Timer(1000, e -> {
            clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });
        clockTimer.start();

        JLabel userBadge = new JLabel(currentUser.getFullName() + " " + currentUser.getRoleBadge());
        userBadge.setFont(Theme.FONT_BOLD);
        userBadge.setForeground(new Color(129, 140, 248));
        right.add(userBadge);

        JButton switchBtn = Theme.createButton("Switch User", new Color(51, 65, 85), Color.WHITE);
        switchBtn.addActionListener(e -> {
            if (deadlineDaemon != null) {
                deadlineDaemon.stopMonitoring();
            }
            dispose();
            if (onSwitchUser != null) {
                onSwitchUser.run();
            }
        });
        right.add(switchBtn);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout(10, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        statusLabel = new JLabel("Status: Initialized | DB: SQLite (smartflow.db) | Background Daemon: Active");
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_MUTED);

        JLabel copyright = new JLabel("SmartFlow Major Project  -  Java OOP, Swing, Multithreading & JDBC");
        copyright.setFont(Theme.FONT_SMALL);
        copyright.setForeground(Theme.TEXT_MUTED);

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(copyright, BorderLayout.EAST);
        return footer;
    }

    private void startBackgroundServices() {
        // Background Thread (Deadline Monitor Daemon)
        deadlineDaemon = new DeadlineMonitorDaemon(taskService, urgentTasks -> {
            long overdueCount = urgentTasks.stream().filter(Task::isOverdue).count();
            long dueSoonCount = urgentTasks.size() - overdueCount;

            String msg;
            if (overdueCount > 0) {
                msg = "[ALERT] " + overdueCount + " task(s) are OVERDUE! " + (dueSoonCount > 0 ? (" (" + dueSoonCount + " approaching deadline)") : "");
                dashboardTab.setAlertMessage(msg, true);
            } else if (dueSoonCount > 0) {
                msg = "[NOTICE] " + dueSoonCount + " task(s) approaching deadline within 48 hours.";
                dashboardTab.setAlertMessage(msg, true);
            } else {
                msg = "Background Deadline Monitor active. All deadlines on track.";
                dashboardTab.setAlertMessage(msg, false);
            }
        }, 15000); // Check every 15 seconds

        deadlineDaemon.start();
    }

    public void refreshAllData() {
        try {
            List<Task> tasks = taskService.getAllTasks();
            List<Resource> resources = resourceService.getAllResources();

            dashboardTab.updateData(tasks, resources);
            tasksTab.reloadData(tasks);
            resourcesTab.reloadData(resources);

            statusLabel.setText(String.format("Status: Ready | Total Tasks: %d | Active Resources: %d | DB: Connected",
                    tasks.size(), resources.size()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to refresh data: " + e.getMessage(), "Sync Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
