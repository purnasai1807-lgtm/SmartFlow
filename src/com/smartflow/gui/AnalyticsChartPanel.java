package com.smartflow.gui;

import com.smartflow.model.*;
import com.smartflow.service.AnalyticsService;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Custom Graphics2D component.
 * Paints anti-aliased vector charts visualizing project analytics:
 * 1. Overview: Status Distribution & Resource Capacity Donut
 * 2. Team Workload: Active task distribution across team members
 * 3. Priority & Risk: Priority distribution & Project Health Gauge
 * 4. Cost Analysis: Hourly burn rate by resource category
 */
public class AnalyticsChartPanel extends JPanel {
    public static final int VIEW_OVERVIEW = 0;
    public static final int VIEW_WORKLOAD = 1;
    public static final int VIEW_PRIORITY = 2;
    public static final int VIEW_COST = 3;

    private final AnalyticsService analyticsService;
    private List<Task> tasks;
    private List<Resource> resources;
    private int currentView = VIEW_OVERVIEW;

    private JButton btnOverview;
    private JButton btnWorkload;
    private JButton btnPriority;
    private JButton btnCost;

    public AnalyticsChartPanel(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 360));
        setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));

        initHeaderSelector();
    }

    private void initHeaderSelector() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(new Color(248, 250, 252));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        JLabel title = new JLabel("Live Analytics: ");
        title.setFont(Theme.FONT_BOLD);
        title.setForeground(Theme.TEXT_MAIN);
        bar.add(title);

        btnOverview = createViewButton("Status & Resources", VIEW_OVERVIEW);
        btnWorkload = createViewButton("Team Workload", VIEW_WORKLOAD);
        btnPriority = createViewButton("Priority & Health", VIEW_PRIORITY);
        btnCost = createViewButton("Cost Breakdown", VIEW_COST);

        bar.add(btnOverview);
        bar.add(btnWorkload);
        bar.add(btnPriority);
        bar.add(btnCost);

        updateButtonStyles();
        add(bar, BorderLayout.NORTH);
    }

    private JButton createViewButton(String text, int viewIndex) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_SMALL);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            currentView = viewIndex;
            updateButtonStyles();
            repaint();
        });
        return btn;
    }

    private void updateButtonStyles() {
        JButton[] buttons = {btnOverview, btnWorkload, btnPriority, btnCost};
        for (int i = 0; i < buttons.length; i++) {
            if (i == currentView) {
                buttons[i].setBackground(Theme.PRIMARY);
                buttons[i].setForeground(Color.WHITE);
                buttons[i].setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            } else {
                buttons[i].setBackground(Color.WHITE);
                buttons[i].setForeground(Theme.TEXT_MAIN);
                buttons[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                        BorderFactory.createEmptyBorder(4, 10, 4, 10)
                ));
            }
        }
    }

    public void updateData(List<Task> tasks, List<Resource> resources) {
        this.tasks = tasks;
        this.resources = resources;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (tasks == null || resources == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int topOffset = 45; // Below header bar
        int width = getWidth();
        int height = getHeight() - topOffset;

        switch (currentView) {
            case VIEW_WORKLOAD:
                drawTeamWorkloadChart(g2, 20, topOffset + 10, width - 40, height - 20);
                break;
            case VIEW_PRIORITY:
                int half = width / 2;
                drawPriorityBarChart(g2, 20, topOffset + 10, half - 30, height - 20);
                g2.setColor(Theme.BORDER);
                g2.drawLine(half, topOffset + 10, half, topOffset + height - 10);
                drawProjectHealthGauge(g2, half + 20, topOffset + 10, half - 40, height - 20);
                break;
            case VIEW_COST:
                drawResourceCostChart(g2, 20, topOffset + 10, width - 40, height - 20);
                break;
            case VIEW_OVERVIEW:
            default:
                int halfW = width / 2;
                drawTaskStatusBarChart(g2, 20, topOffset + 10, halfW - 30, height - 20);
                g2.setColor(Theme.BORDER);
                g2.drawLine(halfW, topOffset + 10, halfW, topOffset + height - 10);
                drawResourceDonutChart(g2, halfW + 20, topOffset + 10, halfW - 40, height - 20);
                break;
        }
    }

    private void drawTaskStatusBarChart(Graphics2D g2, int x, int y, int w, int h) {
        g2.setFont(Theme.FONT_HEADER);
        g2.setColor(Theme.TEXT_MAIN);
        g2.drawString("Tasks by Status", x, y + 16);

        Map<TaskStatus, Long> counts = analyticsService.getTaskCountByStatus(tasks);
        long maxVal = Math.max(1, counts.values().stream().mapToLong(Long::longValue).max().orElse(1));

        TaskStatus[] statuses = TaskStatus.values();
        Color[] barColors = {
                new Color(148, 163, 184), // Backlog
                new Color(59, 130, 246),  // In Progress
                new Color(245, 158, 11),  // Review
                new Color(16, 185, 129)   // Completed
        };

        int chartBottom = y + h - 35;
        int chartTop = y + 45;
        int chartHeight = chartBottom - chartTop;
        int barWidth = Math.max(28, (w - 60) / statuses.length - 15);

        g2.setColor(Theme.BORDER);
        g2.drawLine(x, chartBottom, x + w, chartBottom);

        for (int i = 0; i < statuses.length; i++) {
            TaskStatus status = statuses[i];
            long count = counts.getOrDefault(status, 0L);
            int barHeight = (int) ((count / (double) maxVal) * (chartHeight - 20));
            int barX = x + 25 + i * (barWidth + 15);
            int barY = chartBottom - barHeight;

            g2.setColor(barColors[i]);
            g2.fillRoundRect(barX, barY, barWidth, Math.max(4, barHeight), 6, 6);

            g2.setFont(Theme.FONT_BOLD);
            g2.setColor(Theme.TEXT_MAIN);
            String countStr = String.valueOf(count);
            int strW = g2.getFontMetrics().stringWidth(countStr);
            g2.drawString(countStr, barX + (barWidth - strW) / 2, barY - 6);

            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_MUTED);
            String label = status.getDisplayName();
            int labelW = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, barX + (barWidth - labelW) / 2, chartBottom + 16);
        }
    }

    private void drawResourceDonutChart(Graphics2D g2, int x, int y, int w, int h) {
        g2.setFont(Theme.FONT_HEADER);
        g2.setColor(Theme.TEXT_MAIN);
        g2.drawString("Resource Capacity & Utilization", x, y + 16);

        int totalCap = resources.stream().mapToInt(Resource::getTotalCapacity).sum();
        int totalAlloc = resources.stream().mapToInt(Resource::getAllocatedQuantity).sum();

        int size = Math.min(w - 160, h - 60);
        int cx = x + 10;
        int cy = y + 40;

        double allocAngle = totalCap > 0 ? (totalAlloc * 360.0 / totalCap) : 0;
        double availAngle = 360.0 - allocAngle;

        g2.setColor(new Color(99, 102, 241));
        g2.fillArc(cx, cy, size, size, 90, -(int) Math.round(allocAngle));

        g2.setColor(new Color(226, 232, 240));
        g2.fillArc(cx, cy, size, size, 90 - (int) Math.round(allocAngle), -(int) Math.round(availAngle));

        int holeSize = (int) (size * 0.58);
        int holeOffset = (size - holeSize) / 2;
        g2.setColor(Color.WHITE);
        g2.fillOval(cx + holeOffset, cy + holeOffset, holeSize, holeSize);

        int utilPercent = totalCap > 0 ? (int) Math.round((totalAlloc * 100.0) / totalCap) : 0;
        g2.setFont(Theme.FONT_TITLE);
        g2.setColor(Theme.TEXT_MAIN);
        String pctStr = utilPercent + "%";
        int pctW = g2.getFontMetrics().stringWidth(pctStr);
        g2.drawString(pctStr, cx + (size - pctW) / 2, cy + (size / 2) + 6);

        // Legend
        int legX = cx + size + 20;
        int legY = cy + 20;

        g2.setColor(new Color(99, 102, 241));
        g2.fillRoundRect(legX, legY, 12, 12, 3, 3);
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_MAIN);
        g2.drawString("Allocated (" + totalAlloc + ")", legX + 18, legY + 10);

        g2.setColor(new Color(203, 213, 225));
        g2.fillRoundRect(legX, legY + 24, 12, 12, 3, 3);
        g2.setColor(Theme.TEXT_MAIN);
        g2.drawString("Available (" + Math.max(0, totalCap - totalAlloc) + ")", legX + 18, legY + 34);

        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString("Total Capacity: " + totalCap, legX, legY + 60);
    }

    private void drawTeamWorkloadChart(Graphics2D g2, int x, int y, int w, int h) {
        g2.setFont(Theme.FONT_HEADER);
        g2.setColor(Theme.TEXT_MAIN);
        g2.drawString("Team Workload Distribution (Active Tasks per Member)", x, y + 16);

        Map<String, Long> workloads = analyticsService.getWorkloadPerUser(tasks);
        if (workloads.isEmpty()) {
            g2.setFont(Theme.FONT_REGULAR);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString("No active tasks currently assigned to team members.", x + 20, y + 60);
            return;
        }

        long maxTasks = Math.max(4, workloads.values().stream().mapToLong(Long::longValue).max().orElse(1));
        int startY = y + 45;
        int barHeight = 24;
        int spacing = 16;
        int maxBarWidth = w - 240;

        int idx = 0;
        for (Map.Entry<String, Long> entry : workloads.entrySet()) {
            String member = entry.getKey();
            long count = entry.getValue();
            int currentY = startY + idx * (barHeight + spacing);

            // Member label
            g2.setFont(Theme.FONT_BOLD);
            g2.setColor(Theme.TEXT_MAIN);
            g2.drawString(member, x, currentY + 16);

            // Bar background slot
            int barX = x + 140;
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(barX, currentY, maxBarWidth, barHeight, 6, 6);

            // Actual workload bar
            int fillW = (int) ((count / (double) maxTasks) * maxBarWidth);
            Color barCol = count >= 4 ? Theme.DANGER : (count == 3 ? Theme.WARNING : Theme.SUCCESS);
            g2.setColor(barCol);
            g2.fillRoundRect(barX, currentY, Math.max(8, fillW), barHeight, 6, 6);

            // Count text
            g2.setFont(Theme.FONT_BOLD);
            g2.setColor(Color.WHITE);
            String countText = count + (count == 1 ? " task" : " tasks");
            if (fillW > 60) {
                g2.drawString(countText, barX + 10, currentY + 16);
            } else {
                g2.setColor(Theme.TEXT_MAIN);
                g2.drawString(countText, barX + fillW + 8, currentY + 16);
            }

            // Workload health badge
            g2.setFont(Theme.FONT_SMALL);
            if (count >= 4) {
                g2.setColor(Theme.DANGER);
                g2.drawString("[OVERLOADED]", barX + maxBarWidth + 12, currentY + 16);
            } else if (count == 3) {
                g2.setColor(Theme.WARNING);
                g2.drawString("[Near Capacity]", barX + maxBarWidth + 12, currentY + 16);
            } else {
                g2.setColor(Theme.SUCCESS);
                g2.drawString("[Optimal]", barX + maxBarWidth + 12, currentY + 16);
            }

            idx++;
        }

        // Draw guideline
        g2.setColor(Theme.TEXT_MUTED);
        g2.setFont(Theme.FONT_SMALL);
        g2.drawString("* Reliable Allocation Rule: Members with >= 3 active tasks trigger Workload Conflict Warnings.", x, y + h - 10);
    }

    private void drawPriorityBarChart(Graphics2D g2, int x, int y, int w, int h) {
        g2.setFont(Theme.FONT_HEADER);
        g2.setColor(Theme.TEXT_MAIN);
        g2.drawString("Tasks by Priority Level", x, y + 16);

        Map<TaskPriority, Long> counts = analyticsService.getTaskCountByPriority(tasks);
        long maxVal = Math.max(1, counts.values().stream().mapToLong(Long::longValue).max().orElse(1));

        TaskPriority[] priorities = TaskPriority.values();
        Color[] pColors = {
                new Color(148, 163, 184), // LOW (Slate)
                new Color(59, 130, 246),  // MEDIUM (Blue)
                new Color(249, 115, 22),  // HIGH (Orange)
                new Color(239, 68, 68)    // CRITICAL (Red)
        };

        int chartBottom = y + h - 35;
        int chartTop = y + 45;
        int chartHeight = chartBottom - chartTop;
        int barWidth = Math.max(26, (w - 60) / priorities.length - 15);

        g2.setColor(Theme.BORDER);
        g2.drawLine(x, chartBottom, x + w, chartBottom);

        for (int i = 0; i < priorities.length; i++) {
            TaskPriority p = priorities[i];
            long count = counts.getOrDefault(p, 0L);
            int barHeight = (int) ((count / (double) maxVal) * (chartHeight - 20));
            int barX = x + 25 + i * (barWidth + 15);
            int barY = chartBottom - barHeight;

            g2.setColor(pColors[i]);
            g2.fillRoundRect(barX, barY, barWidth, Math.max(4, barHeight), 6, 6);

            g2.setFont(Theme.FONT_BOLD);
            g2.setColor(Theme.TEXT_MAIN);
            String countStr = String.valueOf(count);
            int strW = g2.getFontMetrics().stringWidth(countStr);
            g2.drawString(countStr, barX + (barWidth - strW) / 2, barY - 6);

            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_MUTED);
            String label = p.name();
            int labelW = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, barX + (barWidth - labelW) / 2, chartBottom + 16);
        }
    }

    private void drawProjectHealthGauge(Graphics2D g2, int x, int y, int w, int h) {
        g2.setFont(Theme.FONT_HEADER);
        g2.setColor(Theme.TEXT_MAIN);
        g2.drawString("Project Health Score", x, y + 16);

        int score = analyticsService.getProjectHealthScore(tasks);
        int size = Math.min(w - 100, h - 70);
        int cx = x + (w - size) / 2 - 20;
        int cy = y + 40;

        // Draw track
        g2.setStroke(new BasicStroke(16, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(241, 245, 249));
        g2.drawArc(cx, cy, size, size, -30, 240);

        // Draw gauge arc
        Color scoreColor = score >= 80 ? Theme.SUCCESS : (score >= 50 ? Theme.WARNING : Theme.DANGER);
        g2.setColor(scoreColor);
        int sweep = (int) Math.round((score / 100.0) * 240);
        g2.drawArc(cx, cy, size, size, 210, -sweep);

        // Center score text
        g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
        g2.setColor(Theme.TEXT_MAIN);
        String scoreStr = score + "%";
        int sW = g2.getFontMetrics().stringWidth(scoreStr);
        g2.drawString(scoreStr, cx + (size - sW) / 2, cy + (size / 2) + 8);

        // Subtitle badge
        g2.setFont(Theme.FONT_BOLD);
        g2.setColor(scoreColor);
        String statusText = score >= 80 ? "HEALTHY" : (score >= 50 ? "MODERATE RISK" : "CRITICAL RISK");
        int stW = g2.getFontMetrics().stringWidth(statusText);
        g2.drawString(statusText, cx + (size - stW) / 2, cy + (size / 2) + 32);

        // Metrics note
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        List<Task> overdue = analyticsService.getOverdueTasks(tasks);
        g2.drawString("Overdue Tasks: " + overdue.size() + " | Active: " + tasks.size(), x, y + h - 10);
    }

    private void drawResourceCostChart(Graphics2D g2, int x, int y, int w, int h) {
        g2.setFont(Theme.FONT_HEADER);
        g2.setColor(Theme.TEXT_MAIN);
        g2.drawString("Active Resource Cost by Category ($/hr)", x, y + 16);

        Map<ResourceType, Double> costMap = analyticsService.getCostByResourceType(resources);
        double totalCost = costMap.values().stream().mapToDouble(Double::doubleValue).sum();
        double maxCost = Math.max(10.0, costMap.values().stream().mapToDouble(Double::doubleValue).max().orElse(10.0));

        ResourceType[] types = ResourceType.values();
        Color[] colors = {
                new Color(59, 130, 246),
                new Color(16, 185, 129),
                new Color(139, 92, 246),
                new Color(245, 158, 11)
        };

        int startY = y + 45;
        int barH = 26;
        int spacing = 18;
        int maxW = w - 220;

        for (int i = 0; i < types.length; i++) {
            ResourceType type = types[i];
            double cost = costMap.getOrDefault(type, 0.0);
            int curY = startY + i * (barH + spacing);

            g2.setFont(Theme.FONT_BOLD);
            g2.setColor(Theme.TEXT_MAIN);
            g2.drawString(type.name(), x, curY + 17);

            // Bar background
            int bX = x + 120;
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(bX, curY, maxW, barH, 6, 6);

            // Filled bar
            int fillW = (int) ((cost / maxCost) * maxW);
            g2.setColor(colors[i]);
            g2.fillRoundRect(bX, curY, Math.max(6, fillW), barH, 6, 6);

            // Value text
            g2.setFont(Theme.FONT_BOLD);
            g2.setColor(Theme.TEXT_MAIN);
            String valStr = String.format("$%.2f/hr", cost);
            g2.drawString(valStr, bX + Math.max(fillW + 8, 10), curY + 17);
        }

        g2.setFont(Theme.FONT_BOLD);
        g2.setColor(Theme.PRIMARY);
        g2.drawString(String.format("Total Hourly Burn Rate: $%.2f/hr  (Active Allocations: %d units)",
                totalCost, resources.stream().mapToInt(Resource::getAllocatedQuantity).sum()), x, y + h - 10);
    }
}
