package com.guardian;

import com.formdev.flatlaf.FlatDarkLaf;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class Dashboard extends JFrame {
    private JPanel contentPanel;
    private JLabel statusLabel;
    private JLabel shieldIconLabel;
    private JTextArea logArea;
    private boolean isLocked;
    
    private Consumer<Boolean> onStatusChange;
    private Runnable onRefresh;

    private JTextField tokenField;
    private JTextField chatIdField;
    private JPasswordField passField;
    private Consumer<JSONObject> onSettingsSave;
    private String currentPassword;

    public Dashboard(boolean initialLockedState, String token, String id, String password) {
        this.isLocked = initialLockedState;
        this.currentPassword = password;
        
        // Apply Modern Dark Theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ignored) {}

        setTitle("Cyber Guardian Security Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Sidebar ---
        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(30, 31, 34));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setLayout(new GridLayout(10, 1, 0, 5));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        try {
            ImageIcon logoIcon = new ImageIcon(getClass().getResource("/icon.png"));
            Image img = logoIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(img));
            logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            sidebar.add(logoLabel);
        } catch (Exception e) {
            JLabel title = new JLabel("GUARDIAN");
            title.setForeground(Color.CYAN);
            title.setFont(new Font("Segoe UI", Font.BOLD, 22));
            title.setHorizontalAlignment(SwingConstants.CENTER);
            sidebar.add(title);
        }

        sidebar.add(createNavButton("🏠 Dashboard", "home"));
        sidebar.add(createNavButton("📜 Activity Logs", "logs"));
        sidebar.add(createNavButton("⚙️ Settings", "settings"));

        add(sidebar, BorderLayout.WEST);

        // --- Main Content Area ---
        contentPanel = new JPanel(new CardLayout());
        contentPanel.setBackground(new Color(43, 45, 48));
        
        contentPanel.add(createHomePanel(), "home");
        contentPanel.add(createLogsPanel(), "logs");
        contentPanel.add(createSettingsPanel(token, id, password), "settings");
        
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createSettingsPanel(String token, String id, String password) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(43, 45, 48));
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 10, 0);

        JLabel head = new JLabel("System Configuration");
        head.setFont(new Font("Segoe UI", Font.BOLD, 22));
        head.setForeground(Color.CYAN);
        panel.add(head, gbc);

        gbc.gridy++;
        panel.add(new JLabel("Telegram Bot Token:"), gbc);
        tokenField = new JTextField(token, 30);
        gbc.gridy++;
        panel.add(tokenField, gbc);

        gbc.gridy++;
        panel.add(new JLabel("Telegram Chat ID:"), gbc);
        chatIdField = new JTextField(id, 30);
        gbc.gridy++;
        panel.add(chatIdField, gbc);

        gbc.gridy++;
        panel.add(new JLabel("Master Password:"), gbc);
        passField = new JPasswordField(password, 30);
        gbc.gridy++;
        panel.add(passField, gbc);

        // Show Password Checkbox
        gbc.gridy++;
        JCheckBox showPass = new JCheckBox("Show Password");
        showPass.setBackground(new Color(43, 45, 48));
        showPass.setForeground(Color.LIGHT_GRAY);
        showPass.addActionListener(e -> {
            if (showPass.isSelected()) {
                passField.setEchoChar((char) 0);
            } else {
                passField.setEchoChar('•');
            }
        });
        panel.add(showPass, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(30, 0, 0, 0);
        JButton saveBtn = new JButton("Save & Apply Settings");
        saveBtn.setPreferredSize(new Dimension(200, 40));
        saveBtn.addActionListener(e -> {
            System.out.println("DEBUG: Save Button Clicked!");
            if (onSettingsSave != null) {
                JSONObject settings = new JSONObject();
                settings.put("botToken", tokenField.getText());
                settings.put("chatId", chatIdField.getText());
                settings.put("password", new String(passField.getPassword()));
                onSettingsSave.accept(settings);
                this.currentPassword = new String(passField.getPassword());
                JOptionPane.showMessageDialog(this, "Settings Saved Successfully!");
            } else {
                System.out.println("DEBUG: onSettingsSave is NULL!");
                JOptionPane.showMessageDialog(this, "Error: Settings callback not initialized!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(saveBtn, gbc);

        return panel;
    }

    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(43, 45, 48));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.CENTER;

        // Large Shield Icon
        shieldIconLabel = new JLabel(isLocked ? "🔒" : "🔓");
        shieldIconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 120));
        shieldIconLabel.setForeground(isLocked ? Color.RED : Color.GREEN);
        gbc.gridy = 0;
        panel.add(shieldIconLabel, gbc);

        // Status Text
        gbc.gridy = 1;
        statusLabel = new JLabel(isLocked ? "SYSTEM LOCKED" : "SYSTEM UNLOCKED");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        statusLabel.setForeground(Color.WHITE);
        gbc.insets = new Insets(20, 0, 40, 0);
        panel.add(statusLabel, gbc);

        // Toggle Button
        gbc.gridy = 2;
        JButton toggleBtn = new JButton(isLocked ? "Unlock Security" : "Lock Security");
        toggleBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        toggleBtn.setPreferredSize(new Dimension(250, 50));
        toggleBtn.setBackground(isLocked ? new Color(70, 0, 0) : new Color(0, 70, 0));
        toggleBtn.setForeground(Color.WHITE);
        toggleBtn.addActionListener(e -> {
            if (isLocked) {
                // Ask for password to unlock
                JPasswordField pf = new JPasswordField();
                int okCxl = JOptionPane.showConfirmDialog(this, pf, "Enter Master Password to UNLOCK:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (okCxl == JOptionPane.OK_OPTION) {
                    String input = new String(pf.getPassword());
                    if (input.equals(currentPassword)) {
                        if (onStatusChange != null) onStatusChange.accept(false);
                    } else {
                        JOptionPane.showMessageDialog(this, "Incorrect Password!", "Access Denied", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                // Instantly lock
                if (onStatusChange != null) onStatusChange.accept(true);
            }
        });
        panel.add(toggleBtn, gbc);

        return panel;
    }

    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(43, 45, 48));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel logTitle = new JLabel("Live Security Activity");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logTitle.setForeground(Color.LIGHT_GRAY);
        panel.add(logTitle, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 31, 34));
        logArea.setForeground(new Color(180, 180, 180));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(Color.LIGHT_GRAY);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addActionListener(e -> {
            if (cardName.equals("settings")) {
                if (tokenField.getText().isEmpty()) {
                    // Allow setup if empty
                    CardLayout cl = (CardLayout) contentPanel.getLayout();
                    cl.show(contentPanel, cardName);
                } else {
                    // Ask for password securely
                    JPasswordField pf = new JPasswordField();
                    int okCxl = JOptionPane.showConfirmDialog(this, pf, "Enter Master Password to access Settings:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                    if (okCxl == JOptionPane.OK_OPTION) {
                        String input = new String(pf.getPassword());
                        if (input.equals(currentPassword)) {
                            CardLayout cl = (CardLayout) contentPanel.getLayout();
                            cl.show(contentPanel, cardName);
                        } else {
                            JOptionPane.showMessageDialog(this, "Incorrect Password!", "Access Denied", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            } else {
                CardLayout cl = (CardLayout) contentPanel.getLayout();
                cl.show(contentPanel, cardName);
            }
        });
        
        return btn;
    }

    public void updateStatus(boolean locked) {
        this.isLocked = locked;
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(locked ? "SYSTEM LOCKED" : "SYSTEM UNLOCKED");
            shieldIconLabel.setText(locked ? "🔒" : "🔓");
            shieldIconLabel.setForeground(locked ? Color.RED : Color.GREEN);
            
            // Update Home Panel Button (needs reference to button)
            contentPanel.revalidate();
            contentPanel.repaint();
        });
    }

    public void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message + "\n");
        });
    }

    public void setOnStatusChange(Consumer<Boolean> callback) {
        this.onStatusChange = callback;
    }

    public void setOnSettingsSave(Consumer<JSONObject> callback) {
        this.onSettingsSave = callback;
    }

    public void updatePassword(String newPassword) {
        this.currentPassword = newPassword;
    }
}
