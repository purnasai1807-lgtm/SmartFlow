package com.smartflow.gui;

import com.smartflow.exception.DatabaseOperationException;
import com.smartflow.model.Resource;
import com.smartflow.model.User;
import com.smartflow.service.ResourceService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Interactive panel to view, allocate, and manage enterprise resources.
 */
public class ResourcePanel extends JPanel {
    private final ResourceService resourceService;
    private final User currentUser;
    private final Runnable onDataChanged;

    private JTable resourceTable;
    private DefaultTableModel tableModel;
    private List<Resource> currentResources;

    public ResourcePanel(ResourceService resourceService, User currentUser, Runnable onDataChanged) {
        this.resourceService = resourceService;
        this.currentUser = currentUser;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        initToolbar();
        initTable();
    }

    private void initToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JLabel label = new JLabel("Enterprise Hardware, Cloud & Workspace Resources");
        label.setFont(Theme.FONT_HEADER);
        label.setForeground(Theme.TEXT_MAIN);
        bar.add(label, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton addBtn = Theme.createPrimaryButton("+ Add Resource");
        addBtn.setEnabled(currentUser.canManageResources());
        addBtn.addActionListener(e -> openResourceDialog(null));
        actions.add(addBtn);

        JButton editBtn = Theme.createButton("Edit", Theme.BORDER, Theme.TEXT_MAIN);
        editBtn.setEnabled(currentUser.canManageResources());
        editBtn.addActionListener(e -> onEditSelected());
        actions.add(editBtn);

        JButton delBtn = Theme.createDangerButton("Delete");
        delBtn.setEnabled(currentUser.canManageResources());
        delBtn.addActionListener(e -> onDeleteSelected());
        actions.add(delBtn);

        bar.add(actions, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] cols = {"ID", "Resource Name", "Type", "Capacity", "Allocated", "Available", "Cost ($/hr)", "Utilization %", "Location"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        resourceTable = new JTable(tableModel);
        resourceTable.setRowHeight(28);
        resourceTable.setFont(Theme.FONT_REGULAR);
        resourceTable.getTableHeader().setFont(Theme.FONT_BOLD);
        resourceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        resourceTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(Theme.FONT_BOLD);
                double val = value instanceof Number ? ((Number) value).doubleValue() : 0.0;
                if (val >= 80.0) {
                    l.setForeground(Theme.DANGER);
                } else if (val >= 50.0) {
                    l.setForeground(Theme.WARNING);
                } else {
                    l.setForeground(Theme.SUCCESS);
                }
                return l;
            }
        });

        resourceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && currentUser.canManageResources()) {
                    onEditSelected();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(resourceTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(scroll, BorderLayout.CENTER);
    }

    public void reloadData(List<Resource> resources) {
        this.currentResources = resources;
        tableModel.setRowCount(0);
        for (Resource r : resources) {
            tableModel.addRow(new Object[]{
                    r.getId(),
                    r.getName(),
                    r.getType().getLabel(),
                    r.getTotalCapacity(),
                    r.getAllocatedQuantity(),
                    r.getAvailableQuantity(),
                    String.format("$%.2f", r.getHourlyCost()),
                    String.format("%.1f%%", r.getUtilizationPercentage()),
                    r.getLocation() != null ? r.getLocation() : ""
            });
        }
    }

    private Resource getSelectedResource() {
        int row = resourceTable.getSelectedRow();
        if (row == -1) return null;
        int id = (int) tableModel.getValueAt(row, 0);
        return currentResources.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
    }

    private void onEditSelected() {
        Resource r = getSelectedResource();
        if (r != null) {
            openResourceDialog(r);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a resource to edit.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onDeleteSelected() {
        Resource r = getSelectedResource();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Please select a resource to delete.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete resource #" + r.getId() + " (" + r.getName() + ")?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                resourceService.deleteResource(r.getId(), currentUser);
                if (onDataChanged != null) onDataChanged.run();
            } catch (DatabaseOperationException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting resource: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openResourceDialog(Resource r) {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        ResourceDialog dlg = new ResourceDialog(parent, resourceService, currentUser, r);
        dlg.setVisible(true);
        if (dlg.isSaved() && onDataChanged != null) {
            onDataChanged.run();
        }
    }
}
