package com.smartflow.gui;

import com.smartflow.dao.RecordDao;
import com.smartflow.dao.UserDao;
import com.smartflow.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interactive Login Screen showcasing different User Roles.
 */
public class LoginFrame extends JFrame {
    private final UserDao userDao;
    private final RecordDao recordDao;
    private final Consumer<User> onLoginSuccess;

    public LoginFrame(UserDao userDao, RecordDao recordDao, Consumer<User> onLoginSuccess) {
        super("SmartFlow - Login & Role Selection");
        this.userDao = userDao;
        this.recordDao = recordDao;
        this.onLoginSuccess = onLoginSuccess;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel main = new JPanel(new BorderLayout(16, 16));
        main.setBackground(Theme.BG_LIGHT);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 4, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("SmartFlow Workspace", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Theme.PRIMARY);

        JLabel subtitle = new JLabel("Select an enterprise role to log in", SwingConstants.CENTER);
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_MUTED);

        header.add(title);
        header.add(subtitle);
        main.add(header, BorderLayout.NORTH);

        // Role Quick Select Cards
        JPanel rolesPanel = new JPanel(new GridLayout(3, 1, 12, 12));
        rolesPanel.setOpaque(false);

        try {
            List<User> users = userDao.findAll();
            for (User u : users) {
                JPanel card = createRoleCard(u);
                rolesPanel.add(card);
            }
        } catch (Exception e) {
            rolesPanel.add(new JLabel("Failed to load users: " + e.getMessage()));
        }

        main.add(rolesPanel, BorderLayout.CENTER);

        JLabel tip = new JLabel("Click any user profile above to authenticate instantly", SwingConstants.CENTER);
        tip.setFont(Theme.FONT_SMALL);
        tip.setForeground(Theme.TEXT_MUTED);
        main.add(tip, BorderLayout.SOUTH);

        add(main);
    }

    private JPanel createRoleCard(User user) {
        JPanel card = Theme.createCard();
        card.setLayout(new BorderLayout(8, 8));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel nameLbl = new JLabel(user.getFullName() + "  " + user.getRoleBadge());
        nameLbl.setFont(Theme.FONT_BOLD);
        nameLbl.setForeground(Theme.TEXT_MAIN);

        JLabel subLbl = new JLabel("Username: @" + user.getUsername() + "  |  " + user.getEmail());
        subLbl.setFont(Theme.FONT_SMALL);
        subLbl.setForeground(Theme.TEXT_MUTED);

        JButton loginBtn = Theme.createPrimaryButton("Login as " + user.getRole());
        loginBtn.setFont(Theme.FONT_SMALL);
        loginBtn.addActionListener(e -> {
            try {
                if (recordDao != null) {
                    recordDao.log(new com.smartflow.model.SystemRecord("USER_LOGIN", "USER", user.getId(),
                            "User '" + user.getFullName() + "' authenticated with role " + user.getRole(),
                            user.getUsername()));
                }
            } catch (Exception ignored) {}
            dispose();
            onLoginSuccess.accept(user);
        });

        card.add(nameLbl, BorderLayout.NORTH);
        card.add(subLbl, BorderLayout.CENTER);
        card.add(loginBtn, BorderLayout.EAST);

        return card;
    }
}
