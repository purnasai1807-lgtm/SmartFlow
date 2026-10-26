package com.smartflow.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Design system providing a modern, consistent aesthetic for the Swing interface.
 */
public class Theme {
    // Color Palette
    public static final Color BG_DARK = new Color(24, 30, 42);
    public static final Color BG_LIGHT = new Color(248, 250, 252);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color PRIMARY = new Color(79, 70, 229);      // Indigo
    public static final Color PRIMARY_HOVER = new Color(67, 56, 202);
    public static final Color SUCCESS = new Color(16, 185, 129);     // Emerald green
    public static final Color WARNING = new Color(245, 158, 11);     // Amber
    public static final Color DANGER = new Color(239, 68, 68);       // Coral red
    public static final Color TEXT_MAIN = new Color(15, 23, 42);     // Slate 900
    public static final Color TEXT_MUTED = new Color(100, 116, 139); // Slate 500
    public static final Color BORDER = new Color(226, 232, 240);     // Slate 200

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    public static JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1, true),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createPrimaryButton(String text) {
        return createButton(text, PRIMARY, Color.WHITE);
    }

    public static JButton createDangerButton(String text) {
        return createButton(text, DANGER, Color.WHITE);
    }

    public static JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        return card;
    }
}
