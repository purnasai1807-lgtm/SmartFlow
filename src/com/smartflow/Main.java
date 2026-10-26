package com.smartflow;

import com.smartflow.dao.*;
import com.smartflow.gui.LoginFrame;
import com.smartflow.gui.MainFrame;
import com.smartflow.model.User;
import com.smartflow.service.*;

import javax.swing.*;

/**
 * Main Application Launcher for SmartFlow.
 * Bootstraps Services, DAOs, and launches the graphical interface.
 */
public class Main {
    private static TaskService taskService;
    private static ResourceService resourceService;
    private static AnalyticsService analyticsService;
    private static UserDao userDao;
    private static RecordDao recordDao;

    public static void main(String[] args) {
        // Set System Look and Feel for native operating system rendering
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Initialize Persistence & Services (Interchangeable DAOs)
        TaskDao taskDao = new JdbcTaskDao();
        ResourceDao resourceDao = new JdbcResourceDao();
        userDao = new JdbcUserDao();
        recordDao = new JdbcRecordDao();

        taskService = new TaskServiceImpl(taskDao, resourceDao, recordDao);
        resourceService = new ResourceServiceImpl(resourceDao, recordDao);
        analyticsService = new AnalyticsService();

        // Launch UI on Event Dispatch Thread (Swing EDT)
        SwingUtilities.invokeLater(Main::showLogin);
    }

    private static void showLogin() {
        LoginFrame loginFrame = new LoginFrame(userDao, recordDao, Main::launchMainApp);
        loginFrame.setVisible(true);
    }

    private static void launchMainApp(User user) {
        MainFrame mainFrame = new MainFrame(
                user,
                taskService,
                resourceService,
                analyticsService,
                userDao,
                recordDao,
                Main::showLogin
        );
        mainFrame.setVisible(true);
    }
}
