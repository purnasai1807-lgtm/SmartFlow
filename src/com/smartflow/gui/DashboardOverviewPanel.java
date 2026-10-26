package com.smartflow.gui;

import com.smartflow.model.Resource;
import com.smartflow.model.Task;
import com.smartflow.service.AnalyticsService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Executive Dashboard Overview combining KPI Cards and Custom Graphics2D Charts.
 */
public class DashboardOverviewPanel extends JPanel {
    private final AnalyticsService analyticsService;
    private final AnalyticsChartPanel chartPanel;

    private JLabel totalTasksLabel;
    private JLabel completionRateLabel;
    private JLabel overdueLabel;
    private JLabel healthScoreLabel;
    private JLabel resourceCostLabel;
    private JLabel bottleneckLabel;
    private JLabel alertBanner;

    public DashboardOverviewPanel(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        this.chartPanel = new AnalyticsChartPanel(analyticsService);

        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        initKPIBanner();
        add(chartPanel, BorderLayout.CENTER);
    }

    private void initKPIBanner() {
        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setOpaque(false);

        // Alert Banner (updated by background daemon thread)
        alertBanner = new JLabel("  Background Deadline Monitor & Conflict Engine active. All systems nominal.", SwingConstants.LEFT);
        alertBanner.setFont(Theme.FONT_REGULAR);
        alertBanner.setForeground(Theme.TEXT_MUTED);
        alertBanner.setOpaque(true);
        alertBanner.setBackground(new Color(241, 245, 249));
        alertBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        container.add(alertBanner, BorderLayout.NORTH);

        // KPI Metric Cards Grid (6 cards)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 6, 10, 10));
        kpiGrid.setOpaque(false);

        totalTasksLabel = new JLabel("0", SwingConstants.CENTER);
        completionRateLabel = new JLabel("0%", SwingConstants.CENTER);
        overdueLabel = new JLabel("0", SwingConstants.CENTER);
        healthScoreLabel = new JLabel("100%", SwingConstants.CENTER);
        resourceCostLabel = new JLabel("$0.00/hr", SwingConstants.CENTER);
        bottleneckLabel = new JLabel("0", SwingConstants.CENTER);

        kpiGrid.add(createMetricCard("TOTAL TASKS", totalTasksLabel, Theme.PRIMARY));
        kpiGrid.add(createMetricCard("COMPLETION", completionRateLabel, Theme.SUCCESS));
        kpiGrid.add(createMetricCard("URGENT / DUE", overdueLabel, Theme.DANGER));
        kpiGrid.add(createMetricCard("HEALTH SCORE", healthScoreLabel, new Color(14, 165, 233)));
        kpiGrid.add(createMetricCard("RESOURCE COST", resourceCostLabel, new Color(139, 92, 246)));
        kpiGrid.add(createMetricCard("BOTTLENECKS", bottleneckLabel, Theme.WARNING));

        container.add(kpiGrid, BorderLayout.CENTER);
        add(container, BorderLayout.NORTH);
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = Theme.createCard();
        card.setLayout(new BorderLayout(4, 4));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLbl.setForeground(Theme.TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(accent);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void updateData(List<Task> tasks, List<Resource> resources) {
        chartPanel.updateData(tasks, resources);

        int total = tasks.size();
        totalTasksLabel.setText(String.valueOf(total));

        double avgProgress = analyticsService.getAverageProgress(tasks);
        completionRateLabel.setText(String.format("%.1f%%", avgProgress));

        List<Task> overdue = analyticsService.getOverdueTasks(tasks);
        overdueLabel.setText(String.valueOf(overdue.size()));

        int health = analyticsService.getProjectHealthScore(tasks);
        healthScoreLabel.setText(health + "%");
        if (health >= 80) {
            healthScoreLabel.setForeground(Theme.SUCCESS);
        } else if (health >= 50) {
            healthScoreLabel.setForeground(Theme.WARNING);
        } else {
            healthScoreLabel.setForeground(Theme.DANGER);
        }

        double cost = analyticsService.getTotalAllocatedResourceCost(resources);
        resourceCostLabel.setText(String.format("$%.2f/hr", cost));

        long bottlenecks = analyticsService.getBottleneckResourceCount(resources);
        bottleneckLabel.setText(String.valueOf(bottlenecks));
        bottleneckLabel.setForeground(bottlenecks > 0 ? Theme.DANGER : Theme.SUCCESS);
    }

    public void setAlertMessage(String message, boolean isUrgent) {
        alertBanner.setText("  " + message);
        if (isUrgent) {
            alertBanner.setBackground(new Color(254, 242, 242));
            alertBanner.setForeground(Theme.DANGER);
            alertBanner.setBorder(BorderFactory.createLineBorder(new Color(252, 165, 165), 1, true));
        } else {
            alertBanner.setBackground(new Color(241, 245, 249));
            alertBanner.setForeground(Theme.TEXT_MUTED);
            alertBanner.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        }
    }
}
