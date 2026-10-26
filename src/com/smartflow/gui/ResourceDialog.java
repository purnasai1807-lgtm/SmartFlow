package com.smartflow.gui;

import com.smartflow.exception.ValidationException;
import com.smartflow.model.Resource;
import com.smartflow.model.ResourceType;
import com.smartflow.model.User;
import com.smartflow.service.ResourceService;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog to create or edit enterprise resources.
 */
public class ResourceDialog extends JDialog {
    private final ResourceService resourceService;
    private final User currentUser;
    private final Resource existingResource;
    private boolean saved = false;

    private JTextField nameField;
    private JComboBox<ResourceType> typeCombo;
    private JSpinner capacitySpinner;
    private JSpinner costSpinner;
    private JTextField locationField;

    public ResourceDialog(Frame owner, ResourceService resourceService, User currentUser, Resource existingResource) {
        super(owner, existingResource == null ? "Add New Resource" : "Edit Resource #" + existingResource.getId(), true);
        this.resourceService = resourceService;
        this.currentUser = currentUser;
        this.existingResource = existingResource;

        initUI();
        if (existingResource != null) {
            populateFields(existingResource);
        }
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Name
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Resource Name * :"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        panel.add(nameField, gbc);

        // Type
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Resource Type:"), gbc);
        gbc.gridx = 1;
        typeCombo = new JComboBox<>(ResourceType.values());
        panel.add(typeCombo, gbc);

        // Capacity
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Total Capacity * :"), gbc);
        gbc.gridx = 1;
        capacitySpinner = new JSpinner(new SpinnerNumberModel(5, 1, 1000, 1));
        panel.add(capacitySpinner, gbc);

        // Hourly Cost
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Hourly Cost ($):"), gbc);
        gbc.gridx = 1;
        costSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 10000.0, 2.5));
        panel.add(costSpinner, gbc);

        // Location
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Location / Scope:"), gbc);
        gbc.gridx = 1;
        locationField = new JTextField(20);
        panel.add(locationField, gbc);

        add(panel, BorderLayout.CENTER);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonBar.setBackground(Theme.BG_LIGHT);

        JButton cancelBtn = Theme.createButton("Cancel", Theme.BORDER, Theme.TEXT_MAIN);
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = Theme.createPrimaryButton(existingResource == null ? "Add Resource" : "Update Resource");
        saveBtn.addActionListener(e -> onSave());

        buttonBar.add(cancelBtn);
        buttonBar.add(saveBtn);
        add(buttonBar, BorderLayout.SOUTH);
    }

    private void populateFields(Resource r) {
        nameField.setText(r.getName());
        typeCombo.setSelectedItem(r.getType());
        capacitySpinner.setValue(r.getTotalCapacity());
        costSpinner.setValue(r.getHourlyCost());
        locationField.setText(r.getLocation());
    }

    private void onSave() {
        try {
            Resource r = existingResource != null ? existingResource : new Resource();
            r.setName(nameField.getText());
            r.setType((ResourceType) typeCombo.getSelectedItem());
            r.setTotalCapacity((Integer) capacitySpinner.getValue());
            r.setHourlyCost(((Number) costSpinner.getValue()).doubleValue());
            r.setLocation(locationField.getText());

            if (existingResource == null) {
                resourceService.createResource(r, currentUser);
            } else {
                resourceService.updateResource(r, currentUser);
            }

            saved = true;
            dispose();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Notice", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save resource: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
