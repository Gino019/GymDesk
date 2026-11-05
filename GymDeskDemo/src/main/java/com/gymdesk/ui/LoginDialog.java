package com.gymdesk.ui;

import com.gymdesk.domain.User;
import com.gymdesk.service.GymService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Diálogo modal de inicio de sesión de GymDesk.
 * Solicita credenciales seguras y autentica al usuario contra el servicio.
 */
public class LoginDialog extends JDialog {

    private final GymService service;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();

    private final Color navy = new Color(20, 31, 48);
    private final Color blue = new Color(40, 112, 220);
    private final Color bg = new Color(245, 247, 250);

    private boolean authenticated = false;

    public LoginDialog(JFrame parent, GymService service) {
        super(parent, "GymDesk | Iniciar Sesión", true);
        this.service = service;

        setSize(440, 370);
        setResizable(false);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(bg);

        mainPanel.add(createHeader(), BorderLayout.NORTH);
        mainPanel.add(createForm(), BorderLayout.CENTER);
        mainPanel.add(createFooter(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setBackground(navy);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(22, 24, 20, 24));

        JLabel title = new JLabel("GYMDESK");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Control Administrativo y Acceso");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(195, 210, 230));
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);

        return header;
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridLayout(4, 1, 6, 6));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(20, 36, 10, 36));

        JLabel userLabel = new JLabel("Nombre de usuario:");
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        userLabel.setForeground(navy);
        usernameField.setPreferredSize(new Dimension(0, 34));
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JLabel passLabel = new JLabel("Contraseña:");
        passLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        passLabel.setForeground(navy);
        passwordField.setPreferredSize(new Dimension(0, 34));
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 13));

        form.add(userLabel);
        form.add(usernameField);
        form.add(passLabel);
        form.add(passwordField);

        return form;
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color bg;
                if (!isEnabled()) {
                    bg = new Color(160, 180, 210);
                } else if (getModel().isPressed()) {
                    bg = new Color(25, 85, 175);
                } else if (getModel().isRollover()) {
                    bg = new Color(30, 98, 205);
                } else {
                    bg = blue;
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();

                super.paintComponent(g);
            }
        };
        b.setUI(new BasicButtonUI());
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton secondaryButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color bg;
                if (!isEnabled()) {
                    bg = new Color(245, 247, 250);
                } else if (getModel().isPressed()) {
                    bg = new Color(225, 232, 240);
                } else if (getModel().isRollover()) {
                    bg = new Color(238, 242, 248);
                } else {
                    bg = new Color(240, 243, 248);
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(new Color(205, 215, 228));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setUI(new BasicButtonUI());
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(navy);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(10, 36, 20, 36));

        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);

        JButton exitBtn = secondaryButton("Salir");
        exitBtn.setPreferredSize(new Dimension(0, 38));
        exitBtn.addActionListener(e -> {
            authenticated = false;
            dispose();
        });

        JButton loginBtn = primaryButton("Iniciar sesión");
        loginBtn.setPreferredSize(new Dimension(0, 38));
        loginBtn.addActionListener(e -> attemptLogin());

        buttons.add(exitBtn);
        buttons.add(loginBtn);

        getRootPane().setDefaultButton(loginBtn);

        footer.add(buttons);

        return footer;
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese su nombre de usuario.", "Campo requerido", JOptionPane.WARNING_MESSAGE);
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese su contraseña.", "Campo requerido", JOptionPane.WARNING_MESSAGE);
            passwordField.requestFocus();
            return;
        }

        try {
            User user = service.authenticate(username, password);
            authenticated = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}

