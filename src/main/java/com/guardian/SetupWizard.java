package com.guardian;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class SetupWizard extends JFrame {
    public SetupWizard() {
        setTitle("Cyber Guardian | Setup");
        setSize(450, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(10, 11, 16));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createLineBorder(new Color(0, 242, 254), 1));

        // Logo Space
        JLabel logo = new JLabel("🛡️", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.PLAIN, 100));
        logo.setForeground(new Color(0, 242, 254));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("CYBER GUARDIAN");
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subTitle = new JLabel("Advanced Security Suite");
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subTitle.setForeground(new Color(136, 136, 136));
        subTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea desc = new JTextArea("Protect your PC from unauthorized access. Link your Telegram bot to receive live alerts and control your system remotely.");
        desc.setEditable(false);
        desc.setWrapStyleWord(true);
        desc.setLineWrap(true);
        desc.setOpaque(false);
        desc.setForeground(new Color(200, 200, 200));
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setMaximumSize(new Dimension(350, 100));
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        desc.setMargin(new Insets(20, 20, 20, 20));

        JButton btnSetup = new JButton("Get Started & Setup Account");
        btnSetup.setBackground(new Color(0, 242, 254));
        btnSetup.setForeground(new Color(10, 11, 16));
        btnSetup.setFocusPainted(false);
        btnSetup.setFont(new Font("Segoe UI Bold", Font.PLAIN, 16));
        btnSetup.setPreferredSize(new Dimension(300, 50));
        btnSetup.setMaximumSize(new Dimension(300, 50));
        btnSetup.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSetup.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnSetup.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:8081/signup"));
                JOptionPane.showMessageDialog(this, "Opening setup page in your browser. After signup, restart the app!");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        panel.add(Box.createVerticalGlue());
        panel.add(logo);
        panel.add(Box.createVerticalStrut(20));
        panel.add(title);
        panel.add(subTitle);
        panel.add(Box.createVerticalStrut(40));
        panel.add(desc);
        panel.add(Box.createVerticalStrut(40));
        panel.add(btnSetup);
        panel.add(Box.createVerticalGlue());

        add(panel);
    }
}
