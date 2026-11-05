package com.gymdesk.ui;

import com.gymdesk.domain.Attendance;
import com.gymdesk.domain.AuditLog;
import com.gymdesk.domain.Exercise;
import com.gymdesk.domain.GymConfig;
import com.gymdesk.domain.Member;
import com.gymdesk.domain.Membership;
import com.gymdesk.domain.Offer;
import com.gymdesk.domain.Payment;
import com.gymdesk.domain.Plan;
import com.gymdesk.domain.ReportSummary;
import com.gymdesk.domain.Routine;
import com.gymdesk.domain.RoutineExercise;
import com.gymdesk.domain.User;
import com.gymdesk.service.CheckInResult;
import com.gymdesk.service.GymService;
import com.gymdesk.app.GymDeskApp;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ventana principal del sistema GymDesk.
 * Aplica el estilo administrativo clásico: fondo claro, barra lateral azul marino y acentos en azul.
 */
public class MainFrame extends JFrame {

    private final GymService service;
    private final JPanel content = new JPanel(new BorderLayout());

    // Paleta de colores oficial GymDesk
    private final Color navy = new Color(20, 31, 48);
    private final Color blue = new Color(40, 112, 220);
    private final Color bg = new Color(245, 247, 250);

    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private String currentPage = "Panel principal";

    public MainFrame(GymService service) {
        this.service = service;

        setTitle("GymDesk | " + service.config().gymName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1220, 800);
        setMinimumSize(new Dimension(1000, 660));
        setLocationRelativeTo(null);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(bg);

        add(createSidebar(), BorderLayout.WEST);

        content.setBackground(bg);
        add(content, BorderLayout.CENTER);

        showDashboard();
    }

    // ==========================================
    // BARRA LATERAL Y NAVEGACIÓN
    // ==========================================

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(navy);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("  GYMDESK");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 23));
        brand.setBorder(new EmptyBorder(26, 12, 24, 0));
        sidebar.add(brand);

        List<String[]> navItems = new ArrayList<>();
        navItems.add(new String[]{"⌂", "Panel principal"});
        navItems.add(new String[]{"♟", "Socios"});
        navItems.add(new String[]{"▣", "Membresías"});
        navItems.add(new String[]{"▤", "Cobros"});
        navItems.add(new String[]{"▥", "Rutinas"});
        navItems.add(new String[]{"✓", "Asistencia"});
        navItems.add(new String[]{"▧", "Facturación"});
        navItems.add(new String[]{"▦", "Reportes"});

        if (service.isAdmin()) {
            navItems.add(new String[]{"👥", "Usuarios"});
            navItems.add(new String[]{"📜", "Auditoría"});
            navItems.add(new String[]{"💾", "Respaldos"});
            navItems.add(new String[]{"⚙", "Configuración"});
        }

        navButtons.clear();
        for (String[] item : navItems) {
            JButton btn = createSidebarButton(item[0], item[1]);
            navButtons.put(item[1], btn);
            sidebar.add(btn);
        }

        sidebar.add(Box.createVerticalGlue());

        // Tarjeta con información del usuario autenticado
        User current = service.getCurrentUser().orElse(null);
        String name = (current != null) ? current.fullName() : "Usuario";
        String role = (current != null) ? current.role() : "INVITADO";

        JPanel userCard = new JPanel();
        userCard.setLayout(new BoxLayout(userCard, BoxLayout.Y_AXIS));
        userCard.setBackground(new Color(15, 23, 36));
        userCard.setBorder(new EmptyBorder(10, 14, 10, 14));
        userCard.setMaximumSize(new Dimension(200, 85));

        JLabel nameLabel = new JLabel("👤 " + (name.length() > 18 ? name.substring(0, 16) + "..." : name));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        JLabel roleBadge = new JLabel(role);
        roleBadge.setForeground(new Color(110, 175, 255));
        roleBadge.setFont(new Font("SansSerif", Font.BOLD, 11));

        JButton logoutBtn = createLogoutButton();

        userCard.add(nameLabel);
        userCard.add(Box.createVerticalStrut(3));
        userCard.add(roleBadge);
        userCard.add(Box.createVerticalStrut(6));
        userCard.add(logoutBtn);

        sidebar.add(userCard);

        JLabel footer = new JLabel("  GymDesk · v1.5");
        footer.setForeground(new Color(150, 165, 185));
        footer.setBorder(new EmptyBorder(8, 10, 12, 0));
        sidebar.add(footer);

        return sidebar;
    }

    private JButton createSidebarButton(String icon, String pageName) {
        JButton btn = new JButton(icon + "   " + pageName) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                boolean isActive = pageName.equals(currentPage);
                boolean isHover = getModel().isRollover();
                boolean isPressed = getModel().isPressed();

                if (isActive) {
                    g2.setColor(new Color(32, 54, 88));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    // Barra indicadora azul en el borde izquierdo
                    g2.setColor(new Color(59, 130, 246));
                    g2.fillRect(0, 0, 4, getHeight());
                } else if (isPressed) {
                    g2.setColor(new Color(24, 38, 62));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else if (isHover) {
                    g2.setColor(new Color(28, 44, 70));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g2.setColor(navy);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setUI(new BasicButtonUI());
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(220, 42));
        btn.setPreferredSize(new Dimension(220, 42));
        btn.setForeground(pageName.equals(currentPage) ? Color.WHITE : new Color(225, 235, 248));
        btn.setFont(new Font("SansSerif", pageName.equals(currentPage) ? Font.BOLD : Font.PLAIN, 13));
        btn.setBorder(new EmptyBorder(0, 20, 0, 8));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> navigate(pageName));
        return btn;
    }

    private JButton createLogoutButton() {
        JButton logoutBtn = new JButton("Cerrar sesión") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color bg = getModel().isPressed() ? new Color(20, 32, 50)
                         : getModel().isRollover() ? new Color(38, 56, 84)
                         : new Color(28, 42, 64);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                g2.setColor(new Color(55, 80, 115));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        logoutBtn.setUI(new BasicButtonUI());
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        logoutBtn.setForeground(new Color(220, 230, 245));
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setBorder(new EmptyBorder(4, 10, 4, 10));
        logoutBtn.addActionListener(e -> logout());
        return logoutBtn;
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de que desea cerrar la sesión actual?",
                "Cerrar sesión",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            service.logout();
            dispose();
            GymDeskApp.startApplication(service);
        }
    }

    private void navigate(String pageName) {
        this.currentPage = pageName;
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(pageName);
            entry.getValue().setForeground(active ? Color.WHITE : new Color(225, 235, 248));
            entry.getValue().setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 13));
            entry.getValue().repaint();
        }

        switch (pageName) {
            case "Panel principal" -> showDashboard();
            case "Socios" -> showMembers();
            case "Membresías" -> showPlans();
            case "Cobros" -> showPayments();
            case "Rutinas" -> showRoutines();
            case "Asistencia" -> showAttendance();
            case "Facturación" -> showInvoices();
            case "Reportes" -> showReports();
            case "Usuarios" -> showUsers();
            case "Auditoría" -> showAuditLogs();
            case "Respaldos" -> showBackups();
            case "Configuración" -> showConfig();
            default -> showDashboard();
        }
    }

    private void page(String title, String subtitle, JComponent body) {
        content.removeAll();

        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setBackground(bg);
        wrapper.setBorder(new EmptyBorder(24, 28, 22, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(navy);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(new Color(105, 116, 132));
        subtitleLabel.setBorder(new EmptyBorder(5, 0, 0, 0));

        JPanel titleBlock = new JPanel(new BorderLayout());
        titleBlock.setOpaque(false);
        titleBlock.add(titleLabel, BorderLayout.NORTH);
        titleBlock.add(subtitleLabel, BorderLayout.SOUTH);

        header.add(titleBlock, BorderLayout.WEST);
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(body, BorderLayout.CENTER);

        content.add(wrapper);
        content.revalidate();
        content.repaint();
    }

    // ==========================================
    // COMPONENTES REUTILIZABLES DE UI
    // ==========================================

    private JPanel panel() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240)),
                new EmptyBorder(16, 16, 16, 16)
        ));
        return p;
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                g2.dispose();

                super.paintComponent(g);
            }
        };
        b.setUI(new BasicButtonUI());
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
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
                    bg = Color.WHITE;
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);

                g2.setColor(new Color(205, 215, 228));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setUI(new BasicButtonUI());
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(navy);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton dangerButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color bg;
                if (!isEnabled()) {
                    bg = new Color(250, 250, 250);
                } else if (getModel().isPressed()) {
                    bg = new Color(254, 202, 202);
                } else if (getModel().isRollover()) {
                    bg = new Color(254, 226, 226);
                } else {
                    bg = new Color(254, 242, 242);
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);

                g2.setColor(new Color(248, 113, 113));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setUI(new BasicButtonUI());
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(new Color(185, 28, 28));
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel boldLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, size));
        l.setForeground(navy);
        return l;
    }

    private JTable createTable(String[] cols, Object[][] rows) {
        DefaultTableModel model = new DefaultTableModel(rows, cols) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(29);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return table;
    }

    private JPanel tableCard(JTable table, JButton actionButton) {
        JPanel p = panel();
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(new JLabel("Listado de registros"), BorderLayout.WEST);
        if (actionButton != null) {
            top.add(actionButton, BorderLayout.EAST);
        }
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private JPanel createForm(String[] labels, JComponent[] fields) {
        JPanel form = new JPanel(new GridLayout(labels.length, 2, 10, 10));
        form.setBorder(new EmptyBorder(12, 12, 12, 12));
        for (int i = 0; i < labels.length; i++) {
            form.add(new JLabel(labels[i]));
            form.add(fields[i]);
        }
        return form;
    }

    private void alert(String message) {
        JOptionPane.showMessageDialog(this, message, "GymDesk", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==========================================
    // 1. PANEL PRINCIPAL (DASHBOARD)
    // ==========================================

    private void showDashboard() {
        GymConfig cfg = service.config();
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        // Tarjetas superiores de indicadores clave (KPIs)
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.add(createStatCard("SOCIOS REGISTRADOS", service.count("members"), "Personas en el sistema"));
        stats.add(createStatCard("ASISTENCIAS HOY", service.countAttendanceBetween(LocalDate.now().toString(), LocalDate.now().toString()), "Ingresos del día"));
        stats.add(createStatCard("INGRESOS DEL MES", String.format(Locale.US, "%s %.2f", cfg.currency(), service.income()), "Cobros netos efectivos"));
        stats.add(createStatCard("POR VENCER (7 DÍAS)", service.expiringMembers(7).size(), "Socios para renovación"));

        // Barra de accesos directos principales
        JPanel quickBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        quickBar.setOpaque(false);

        JButton regMemberBtn = primaryButton("+ Registrar socio");
        regMemberBtn.addActionListener(e -> showMemberDialog(null));

        JButton regPayBtn = primaryButton("+ Registrar cobro");
        regPayBtn.addActionListener(e -> showPaymentDialog());

        JButton checkInBtn = primaryButton("✓ Control de asistencia");
        checkInBtn.addActionListener(e -> showAttendanceDialog());

        JButton routineBtn = secondaryButton("▥ Asignar rutina");
        routineBtn.addActionListener(e -> showNewRoutineDialog());

        quickBar.add(regMemberBtn);
        quickBar.add(regPayBtn);
        quickBar.add(checkInBtn);
        quickBar.add(routineBtn);

        // Paneles de actividad en vivo lado a lado
        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 0));
        lower.setOpaque(false);

        // Panel Izquierdo: Últimas operaciones de cobro
        JPanel payCard = panel();
        payCard.add(boldLabel("Últimas operaciones de caja", 16), BorderLayout.NORTH);

        List<Payment> allPayments = service.payments();
        List<Payment> recentPayments = allPayments.stream().limit(6).toList();
        Object[][] payRows = new Object[recentPayments.size()][5];
        for (int i = 0; i < recentPayments.size(); i++) {
            Payment p = recentPayments.get(i);
            payRows[i] = new Object[]{
                    p.invoiceNumber(), p.memberName(),
                    String.format(Locale.US, "%s %.2f", cfg.currency(), p.amount()),
                    p.method(), p.status()
            };
        }
        JTable payTable = createTable(new String[]{"Comprobante", "Socio", "Monto", "Método", "Estado"}, payRows);
        payCard.add(new JScrollPane(payTable), BorderLayout.CENTER);

        // Panel Derecho: Asistencias registradas hoy
        JPanel attCard = panel();
        attCard.add(boldLabel("Asistencias registradas hoy", 16), BorderLayout.NORTH);

        String today = LocalDate.now().toString();
        List<Attendance> todayAtt = service.attendance().stream()
                .filter(a -> a.checkDate() != null && a.checkDate().startsWith(today))
                .limit(6)
                .toList();
        Object[][] attRows = new Object[todayAtt.size()][4];
        for (int i = 0; i < todayAtt.size(); i++) {
            Attendance a = todayAtt.get(i);
            String time = (a.checkDate() != null && a.checkDate().length() >= 16) ? a.checkDate().substring(11, 16) : a.checkDate();
            attRows[i] = new Object[]{time, a.memberName(), a.planName(), a.statusAtCheckIn()};
        }
        JTable attTable = createTable(new String[]{"Hora", "Socio", "Plan", "Estado"}, attRows);
        attCard.add(new JScrollPane(attTable), BorderLayout.CENTER);

        lower.add(payCard);
        lower.add(attCard);

        JPanel centerContainer = new JPanel(new BorderLayout(0, 12));
        centerContainer.setOpaque(false);
        centerContainer.add(quickBar, BorderLayout.NORTH);
        centerContainer.add(lower, BorderLayout.CENTER);

        body.add(stats, BorderLayout.NORTH);
        body.add(centerContainer, BorderLayout.CENTER);

        page("Panel principal", "Resumen general y actividad en tiempo real de " + cfg.gymName(), body);
    }

    private JPanel createStatCard(String title, Object value, String desc) {
        JPanel card = panel();
        card.setPreferredSize(new Dimension(170, 125));
        card.add(boldLabel(title, 11), BorderLayout.NORTH);

        JLabel valLabel = boldLabel(String.valueOf(value), 27);
        valLabel.setForeground(blue);
        card.add(valLabel, BorderLayout.CENTER);

        JLabel descLabel = new JLabel(desc);
        descLabel.setForeground(Color.GRAY);
        card.add(descLabel, BorderLayout.SOUTH);
        return card;
    }

    // ==========================================
    // 2. MÓDULO DE SOCIOS
    // ==========================================

    private void showMembers() {
        List<Member> list = service.members();
        Object[][] rows = new Object[list.size()][8];
        for (int i = 0; i < list.size(); i++) {
            Member m = list.get(i);
            rows[i] = new Object[]{
                    m.id(), m.document(), m.fullName(), m.phone(), m.email(), m.plan(), m.status(), m.endDate()
            };
        }

        JTable table = createTable(
                new String[]{"ID", "Documento", "Socio", "Teléfono", "Correo", "Plan actual", "Estado", "Vencimiento"},
                rows
        );

        JButton addBtn = primaryButton("+ Nuevo socio");
        addBtn.addActionListener(e -> showMemberDialog(null));

        JButton profileBtn = primaryButton("👁 Ficha 360° del socio");
        profileBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = (int) table.getValueAt(row, 0);
                service.findMemberById(id).ifPresent(this::showMemberProfileDialog);
            } else {
                alert("Selecciona un socio de la tabla para ver su ficha completa.");
            }
        });

        JButton renewBtn = primaryButton("Renovar membresía");
        renewBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = (int) table.getValueAt(row, 0);
                service.findMemberById(id).ifPresent(this::showRenewMembershipDialog);
            } else {
                alert("Selecciona un socio para renovar su membresía.");
            }
        });

        JButton historyBtn = secondaryButton("Ver historial membresías");
        historyBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = (int) table.getValueAt(row, 0);
                service.findMemberById(id).ifPresent(this::showMembershipHistoryDialog);
            } else {
                alert("Selecciona un socio de la tabla.");
            }
        });

        JButton editBtn = secondaryButton("Editar datos");
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = (int) table.getValueAt(row, 0);
                service.findMemberById(id).ifPresent(this::showMemberDialog);
            } else {
                alert("Selecciona un socio de la tabla.");
            }
        });

        JButton deactivateBtn = dangerButton("Dar de baja");
        deactivateBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = (int) table.getValueAt(row, 0);
                String name = (String) table.getValueAt(row, 2);
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "¿Deseas dar de baja al socio " + name + "?\n(Pasará a estado 'Inactivo' y se conservará todo su historial)",
                        "Confirmar baja lógica",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    service.deactivateMember(id);
                    showMembers();
                }
            } else {
                alert("Selecciona un socio de la tabla.");
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(profileBtn);
        buttons.add(renewBtn);
        buttons.add(historyBtn);
        buttons.add(editBtn);
        buttons.add(deactivateBtn);

        JPanel body = tableCard(table, addBtn);
        body.add(buttons, BorderLayout.SOUTH);

        page("Socios", "Registro, consulta, membresías y mantenimiento de clientes", body);
    }

    private void showMemberDialog(Member old) {
        JTextField docField = new JTextField(old == null ? "" : old.document());
        JTextField nameField = new JTextField(old == null ? "" : old.fullName());
        JTextField phoneField = new JTextField(old == null ? "" : old.phone());
        JTextField emailField = new JTextField(old == null ? "" : old.email());

        JComboBox<String> planCombo = new JComboBox<>(
                service.plans().stream().map(Plan::name).toArray(String[]::new)
        );
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Activo", "Vencido", "Inactivo", "Suspendido"});

        if (old != null) {
            planCombo.setSelectedItem(old.plan());
            statusCombo.setSelectedItem(old.status());
        }

        JPanel form = createForm(
                new String[]{"Documento (DNI/CE)", "Nombre completo", "Teléfono", "Correo", "Plan inicial", "Estado"},
                new JComponent[]{docField, nameField, phoneField, emailField, planCombo, statusCombo}
        );

        int result = JOptionPane.showConfirmDialog(
                this, form, old == null ? "Nuevo socio" : "Editar socio", JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                String selectedPlan = (String) planCombo.getSelectedItem();
                Plan planObj = service.plans().stream()
                        .filter(p -> p.name().equals(selectedPlan))
                        .findFirst()
                        .orElse(null);

                String startDate = (old == null) ? LocalDate.now().toString() : old.startDate();
                int days = (planObj != null) ? planObj.durationDays() : 30;
                String endDate = (old == null) ? LocalDate.now().plusDays(days).toString() : old.endDate();

                Member member = new Member(
                        old == null ? 0 : old.id(),
                        docField.getText().trim(),
                        nameField.getText().trim(),
                        phoneField.getText().trim(),
                        emailField.getText().trim(),
                        (String) statusCombo.getSelectedItem(),
                        selectedPlan,
                        startDate,
                        endDate
                );

                service.saveMember(member);
                showMembers();
            } catch (Exception ex) {
                alert("Error al guardar socio: " + ex.getMessage());
            }
        }
    }

    private void showRenewMembershipDialog(Member member) {
        GymConfig cfg = service.config();
        List<Plan> plans = service.plans();
        if (plans.isEmpty()) {
            alert("No hay planes configurados en el sistema.");
            return;
        }

        JComboBox<Plan> planCombo = new JComboBox<>(plans.toArray(Plan[]::new));
        planCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Plan p) {
                    setText(p.name() + " (" + p.durationDays() + " días) - " + cfg.currency() + " " + String.format(Locale.US, "%.2f", p.price()));
                }
                return this;
            }
        });

        LocalDate calculatedStart = service.calculateMembershipStartDate(member.id(), LocalDate.now());
        JLabel startInfo = new JLabel(calculatedStart.toString() + (calculatedStart.isAfter(LocalDate.now()) ? " (Renovación anticipada)" : " (Inicia hoy)"));
        startInfo.setForeground(blue);

        JCheckBox registerPaymentCheck = new JCheckBox("Registrar cobro de la membresía en Caja ahora", true);
        JComboBox<String> paymentMethodCombo = new JComboBox<>(new String[]{"Efectivo", "Yape/Plin", "Transferencia", "Tarjeta"});

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));
        form.add(new JLabel("Socio:"));
        form.add(new JLabel(member.fullName() + " (DNI: " + member.document() + ")"));
        form.add(new JLabel("Seleccionar Plan:"));
        form.add(planCombo);
        form.add(new JLabel("Fecha de inicio calculada:"));
        form.add(startInfo);
        form.add(new JLabel("Cobro inmediato:"));
        form.add(registerPaymentCheck);
        form.add(new JLabel("Método de pago:"));
        form.add(paymentMethodCombo);

        int result = JOptionPane.showConfirmDialog(this, form, "Renovar membresía - " + member.fullName(), JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                Plan selectedPlan = (Plan) planCombo.getSelectedItem();
                if (selectedPlan == null) throw new IllegalArgumentException("Debe seleccionar un plan.");

                Membership newMembership = service.assignOrRenewMembership(member.id(), selectedPlan.id(), null);

                if (registerPaymentCheck.isSelected()) {
                    service.addPayment(
                            member.id(),
                            selectedPlan.price(),
                            null,
                            (String) paymentMethodCombo.getSelectedItem(),
                            "Renovación " + selectedPlan.name() + " (" + newMembership.startDate() + " al " + newMembership.endDate() + ")"
                    );
                }

                alert("Membresía renovada con éxito.\nVálida del: " + newMembership.startDate() + " hasta: " + newMembership.endDate());
                showMembers();
            } catch (Exception ex) {
                alert("Error al renovar membresía: " + ex.getMessage());
            }
        }
    }

    private void showMembershipHistoryDialog(Member member) {
        GymConfig cfg = service.config();
        List<Membership> history = service.membershipsByMember(member.id());
        Object[][] rows = new Object[history.size()][6];
        for (int i = 0; i < history.size(); i++) {
            Membership ms = history.get(i);
            rows[i] = new Object[]{
                    ms.id(), ms.planName(), ms.startDate(), ms.endDate(),
                    String.format(Locale.US, "%s %.2f", cfg.currency(), ms.pricePaid()), ms.status()
            };
        }

        JTable historyTable = createTable(
                new String[]{"ID", "Plan", "Fecha Inicio", "Fecha Vence", "Precio Pagado", "Estado"},
                rows
        );

        JDialog dialog = new JDialog(this, "Historial de membresías - " + member.fullName(), true);
        dialog.setSize(680, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setBorder(new EmptyBorder(15, 15, 15, 15));
        container.add(new JLabel("Historial completo de contratos y renovaciones para " + member.fullName() + ":"), BorderLayout.NORTH);
        container.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JButton closeBtn = secondaryButton("Cerrar");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(closeBtn);
        container.add(bottom, BorderLayout.SOUTH);

        dialog.add(container);
        dialog.setVisible(true);
    }

    // ==========================================
    // 3. MÓDULO DE PLANES Y PROMOCIONES
    // ==========================================

    private void showPlans() {
        GymConfig cfg = service.config();
        List<Plan> list = service.plans();
        Object[][] rows = new Object[list.size()][5];
        for (int i = 0; i < list.size(); i++) {
            Plan p = list.get(i);
            rows[i] = new Object[]{
                    p.id(), p.name(), p.durationDays() + " días", String.format(Locale.US, "%s %.2f", cfg.currency(), p.price()), p.description()
            };
        }

        JTable table = createTable(new String[]{"ID", "Plan", "Duración", "Precio", "Descripción"}, rows);

        JButton addBtn = primaryButton("+ Nuevo plan");
        addBtn.addActionListener(e -> showPlanDialog(null));

        JButton editBtn = secondaryButton("Editar plan seleccionado");
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = (int) table.getValueAt(row, 0);
                service.findPlanById(id).ifPresent(this::showPlanDialog);
            } else {
                alert("Selecciona un plan de la tabla para editar.");
            }
        });

        JButton offersBtn = primaryButton("Gestionar Ofertas / Promociones");
        offersBtn.addActionListener(e -> showOffersDialog());

        boolean isAdmin = service.isAdmin();
        addBtn.setEnabled(isAdmin);
        editBtn.setEnabled(isAdmin);
        offersBtn.setEnabled(isAdmin);
        if (!isAdmin) {
            addBtn.setToolTipText("Solo administradores pueden crear planes.");
            editBtn.setToolTipText("Solo administradores pueden editar planes.");
            offersBtn.setToolTipText("Solo administradores pueden gestionar promociones.");
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(offersBtn);
        buttons.add(editBtn);

        JPanel card = tableCard(table, addBtn);
        card.add(buttons, BorderLayout.SOUTH);

        page("Membresías y planes", "Configura los tipos de membresía y promociones de descuento", card);
    }

    private void showPlanDialog(Plan old) {
        JTextField nameField = new JTextField(old == null ? "" : old.name());
        JTextField daysField = new JTextField(old == null ? "30" : String.valueOf(old.durationDays()));
        JTextField priceField = new JTextField(old == null ? "80.00" : String.format(Locale.US, "%.2f", old.price()));
        JTextField descField = new JTextField(old == null ? "" : old.description());

        JPanel form = createForm(
                new String[]{"Nombre del plan", "Duración (días)", "Precio", "Descripción"},
                new JComponent[]{nameField, daysField, priceField, descField}
        );

        int result = JOptionPane.showConfirmDialog(
                this, form, old == null ? "Nuevo plan de membresía" : "Editar plan", JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                if (old == null) {
                    service.savePlan(
                            nameField.getText(),
                            Integer.parseInt(daysField.getText().trim()),
                            Double.parseDouble(priceField.getText().trim()),
                            descField.getText()
                    );
                } else {
                    service.updatePlan(
                            old.id(),
                            nameField.getText(),
                            Integer.parseInt(daysField.getText().trim()),
                            Double.parseDouble(priceField.getText().trim()),
                            descField.getText()
                    );
                }
                showPlans();
            } catch (Exception e) {
                alert("Error al guardar plan: " + e.getMessage());
            }
        }
    }

    private void showOffersDialog() {
        GymConfig cfg = service.config();
        JDialog dialog = new JDialog(this, "Gestión de Ofertas y Promociones", true);
        dialog.setSize(840, 440);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        Runnable refresh = () -> {
            List<Offer> list = service.offers();
            Object[][] rows = new Object[list.size()][7];
            for (int i = 0; i < list.size(); i++) {
                Offer o = list.get(i);
                String valor = "PERCENTAGE".equalsIgnoreCase(o.discountType()) ? (o.discountValue() + "%") : String.format(Locale.US, "%s %.2f", cfg.currency(), o.discountValue());
                String planName = "Todos los planes";
                if (o.planId() != null) {
                    planName = service.findPlanById(o.planId()).map(Plan::name).orElse("Plan ID " + o.planId());
                }
                rows[i] = new Object[]{
                        o.id(), o.name(), valor, o.validUntil() != null ? o.validUntil() : "Indefinida",
                        planName, o.active() ? "Activa" : "Inactiva", o.description()
                };
            }

            JTable table = createTable(
                    new String[]{"ID", "Nombre", "Descuento", "Vigencia", "Aplica a", "Estado", "Descripción"},
                    rows
            );

            dialog.getContentPane().removeAll();

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton newBtn = primaryButton("+ Nueva promoción");
            newBtn.addActionListener(e -> {
                showNewOfferDialog();
                dialog.dispose();
                showOffersDialog();
            });

            JButton toggleBtn = secondaryButton("Activar / Desactivar seleccionada");
            toggleBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    int id = (int) table.getValueAt(row, 0);
                    String estado = (String) table.getValueAt(row, 5);
                    service.toggleOfferStatus(id, !"Activa".equals(estado));
                    dialog.dispose();
                    showOffersDialog();
                } else {
                    alert("Selecciona una promoción.");
                }
            });

            top.add(newBtn);
            top.add(toggleBtn);

            dialog.add(top, BorderLayout.NORTH);
            dialog.add(new JScrollPane(table), BorderLayout.CENTER);
            dialog.revalidate();
            dialog.repaint();
        };

        refresh.run();
        dialog.setVisible(true);
    }

    private void showNewOfferDialog() {
        JTextField nameField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Porcentual (%)", "Monto fijo"});
        JTextField valField = new JTextField("15.00");
        JTextField validField = new JTextField(LocalDate.now().plusMonths(2).toString());

        List<Plan> plans = service.plans();
        JComboBox<Object> planCombo = new JComboBox<>();
        planCombo.addItem("Todos los planes");
        for (Plan p : plans) {
            planCombo.addItem(p);
        }
        planCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Plan p) {
                    setText(p.name());
                }
                return this;
            }
        });

        JTextField descField = new JTextField();

        JPanel form = createForm(
                new String[]{"Nombre de promoción", "Tipo de descuento", "Valor", "Válida hasta (YYYY-MM-DD)", "Plan aplicable", "Descripción"},
                new JComponent[]{nameField, typeCombo, valField, validField, planCombo, descField}
        );

        int result = JOptionPane.showConfirmDialog(this, form, "Nueva oferta o promoción", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String type = typeCombo.getSelectedIndex() == 0 ? "PERCENTAGE" : "FIXED";
                Integer planId = (planCombo.getSelectedItem() instanceof Plan p) ? p.id() : null;
                service.saveOffer(
                        nameField.getText(),
                        type,
                        Double.parseDouble(valField.getText().trim()),
                        validField.getText().trim(),
                        planId,
                        descField.getText()
                );
                alert("Promoción registrada exitosamente.");
            } catch (Exception ex) {
                alert("Error al registrar promoción: " + ex.getMessage());
            }
        }
    }

    // ==========================================
    // 4. MÓDULO DE COBROS Y PAGOS (MODELO MIXTO)
    // ==========================================

    private void showPayments() {
        GymConfig cfg = service.config();
        List<Payment> list = service.payments();
        Object[][] rows = new Object[list.size()][10];
        for (int i = 0; i < list.size(); i++) {
            Payment p = list.get(i);
            rows[i] = new Object[]{
                    p.id(), p.invoiceNumber(), p.memberName(),
                    String.format(Locale.US, "%s %.2f", cfg.currency(), p.originalAmount()),
                    String.format(Locale.US, "- %s %.2f", cfg.currency(), p.discountApplied()),
                    String.format(Locale.US, "%s %.2f", cfg.currency(), p.amount()),
                    p.promotionName().isBlank() ? "Ninguna" : p.promotionName(),
                    p.method(), p.date(), p.status()
            };
        }

        JTable table = createTable(
                new String[]{"ID", "Comprobante", "Socio", "Precio Base", "Descuento", "Total Cobrado", "Promoción", "Método", "Fecha", "Estado"},
                rows
        );

        JButton addBtn = primaryButton("+ Registrar cobro");
        addBtn.addActionListener(e -> showPaymentDialog());

        JButton cancelBtn = dangerButton("Anular cobro seleccionado");
        cancelBtn.setEnabled(service.isAdmin());
        if (!service.isAdmin()) {
            cancelBtn.setToolTipText("Acción reservada exclusivamente para el Administrador.");
        }
        cancelBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = (int) table.getValueAt(row, 0);
                String estado = (String) table.getValueAt(row, 9);
                if ("Anulado".equalsIgnoreCase(estado)) {
                    alert("Este cobro ya se encuentra anulado.");
                    return;
                }

                String reason = JOptionPane.showInputDialog(
                        this,
                        "Ingresa el motivo justificado de la anulación para auditoría:",
                        "Anular cobro #" + table.getValueAt(row, 1),
                        JOptionPane.WARNING_MESSAGE
                );

                if (reason != null && !reason.trim().isBlank()) {
                    try {
                        service.cancelPayment(id, reason.trim());
                        alert("Cobro anulado con éxito. Se actualizó el estado para auditoría.");
                        showPayments();
                    } catch (Exception ex) {
                        alert("Error al anular: " + ex.getMessage());
                    }
                }
            } else {
                alert("Selecciona un cobro de la tabla.");
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(cancelBtn);

        JPanel card = tableCard(table, addBtn);
        card.add(buttons, BorderLayout.SOUTH);

        page("Cobros y caja", "Registro de pagos, aplicación de promociones y anulación auditada", card);
    }

    private void showPaymentDialog() {
        GymConfig cfg = service.config();
        List<Member> members = service.members();
        if (members.isEmpty()) {
            alert("Primero debes registrar al menos un socio.");
            return;
        }

        JComboBox<Member> memberCombo = new JComboBox<>(members.toArray(Member[]::new));
        memberCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Member m) {
                    setText(m.fullName() + " · DNI: " + m.document());
                }
                return this;
            }
        });

        JTextField conceptField = new JTextField("Mensualidad");
        JTextField baseAmountField = new JTextField("80.00");

        List<Offer> activeOffers = service.applicableOffers(null);
        JComboBox<Object> offerCombo = new JComboBox<>();
        offerCombo.addItem("Ninguna (Precio regular)");
        for (Offer o : activeOffers) {
            offerCombo.addItem(o);
        }
        offerCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Offer o) {
                    String val = "PERCENTAGE".equalsIgnoreCase(o.discountType()) ? (o.discountValue() + "%") : (cfg.currency() + " " + o.discountValue());
                    setText(o.name() + " (-" + val + ")");
                }
                return this;
            }
        });

        JLabel previewDiscount = new JLabel(cfg.currency() + " 0.00");
        JLabel previewFinal = new JLabel(cfg.currency() + " 80.00");
        previewFinal.setFont(new Font("SansSerif", Font.BOLD, 15));
        previewFinal.setForeground(blue);

        Runnable updateCalculations = () -> {
            try {
                double base = Double.parseDouble(baseAmountField.getText().trim());
                double discount = 0.0;
                if (offerCombo.getSelectedItem() instanceof Offer o) {
                    discount = o.calculateDiscount(base);
                }
                double total = Math.max(0.0, base - discount);
                previewDiscount.setText(String.format(Locale.US, "%s %.2f", cfg.currency(), discount));
                previewFinal.setText(String.format(Locale.US, "%s %.2f", cfg.currency(), total));
            } catch (Exception ignored) {
                previewDiscount.setText(cfg.currency() + " 0.00");
                previewFinal.setText(cfg.currency() + " 0.00");
            }
        };

        baseAmountField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateCalculations.run();
            }
        });
        offerCombo.addActionListener(e -> updateCalculations.run());

        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"Efectivo", "Yape/Plin", "Transferencia", "Tarjeta"});

        JPanel form = createForm(
                new String[]{
                        "Socio", "Concepto", "Importe base", "Oferta / Promoción (Mixto)",
                        "Descuento aplicado", "TOTAL A COBRAR", "Método de pago"
                },
                new JComponent[]{
                        memberCombo, conceptField, baseAmountField, offerCombo,
                        previewDiscount, previewFinal, methodCombo
                }
        );

        int result = JOptionPane.showConfirmDialog(this, form, "Registrar cobro con promociones", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                Member selectedMember = (Member) memberCombo.getSelectedItem();
                if (selectedMember == null) throw new IllegalArgumentException("Debe seleccionar un socio.");

                double base = Double.parseDouble(baseAmountField.getText().trim());
                Integer offerId = (offerCombo.getSelectedItem() instanceof Offer o) ? o.id() : null;

                service.addPayment(
                        selectedMember.id(),
                        base,
                        offerId,
                        (String) methodCombo.getSelectedItem(),
                        conceptField.getText()
                );
                showPayments();
            } catch (Exception e) {
                alert("Error al registrar pago: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 5. MÓDULO DE RUTINAS Y EJERCICIOS
    // ==========================================

    private void showRoutines() {
        List<Routine> list = service.routines();
        Object[][] rows = new Object[list.size()][6];
        for (int i = 0; i < list.size(); i++) {
            Routine r = list.get(i);
            int countEx = r.exercises() != null ? r.exercises().size() : 0;
            rows[i] = new Object[]{
                    r.id(), r.memberName(), r.name(), r.goal(),
                    countEx > 0 ? (countEx + " ejercicios") : "Detalle general",
                    r.createdAt()
            };
        }

        JTable table = createTable(new String[]{"ID", "Socio", "Nombre de Rutina", "Objetivo", "Ejercicios", "Fecha Asignación"}, rows);

        JButton addBtn = primaryButton("+ Asignar rutina estructurada");
        addBtn.addActionListener(e -> showNewRoutineDialog());

        JButton viewBtn = secondaryButton("Ver detalle de rutina");
        viewBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = (int) table.getValueAt(row, 0);
                service.routines().stream().filter(r -> r.id() == id).findFirst().ifPresent(this::showRoutineDetailsDialog);
            } else {
                alert("Selecciona una rutina de la tabla.");
            }
        });

        JButton catalogBtn = primaryButton("Catálogo de Ejercicios");
        catalogBtn.addActionListener(e -> showExerciseCatalogDialog());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(catalogBtn);
        buttons.add(viewBtn);

        JPanel card = tableCard(table, addBtn);
        card.add(buttons, BorderLayout.SOUTH);

        page("Rutinas y ejercicios", "Catálogo maestro y planes de entrenamiento estructurados por socio", card);
    }

    private void showNewRoutineDialog() {
        List<Member> members = service.members();
        if (members.isEmpty()) {
            alert("Primero debes registrar al menos un socio.");
            return;
        }

        JComboBox<Member> memberCombo = new JComboBox<>(members.toArray(Member[]::new));
        memberCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Member m) {
                    setText(m.fullName() + " (DNI: " + m.document() + ")");
                }
                return this;
            }
        });

        JTextField nameField = new JTextField("Rutina Torso / Pierna A");
        JComboBox<String> goalCombo = new JComboBox<>(new String[]{
                "Hipertrofia muscular", "Fuerza máxima", "Pérdida de grasa y definición",
                "Acondicionamiento general", "Rehabilitación y salud"
        });
        JTextField notesField = new JTextField("Calentar 10 min en caminadora antes de iniciar");

        List<Exercise> catalog = service.exercises();
        List<RoutineExercise> routineItems = new ArrayList<>();

        JDialog dialog = new JDialog(this, "Asignar nueva rutina estructurada", true);
        dialog.setSize(740, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel formTop = createForm(
                new String[]{"Socio", "Nombre de rutina", "Objetivo principal", "Observaciones generales"},
                new JComponent[]{memberCombo, nameField, goalCombo, notesField}
        );

        DefaultTableModel itemsModel = new DefaultTableModel(new String[]{"Ejercicio", "Series", "Repeticiones", "Descanso (seg)", "Notas"}, 0);
        JTable itemsTable = new JTable(itemsModel);

        JPanel addExercisePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<Exercise> exCombo = new JComboBox<>(catalog.toArray(Exercise[]::new));
        exCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Exercise ex) {
                    setText(ex.name() + " [" + ex.muscleGroup() + "]");
                }
                return this;
            }
        });
        JTextField seriesField = new JTextField("4", 3);
        JTextField repsField = new JTextField("10-12", 5);
        JTextField restField = new JTextField("60", 4);
        JButton addExBtn = primaryButton("+ Añadir ejercicio");

        addExBtn.addActionListener(e -> {
            Exercise selectedEx = (Exercise) exCombo.getSelectedItem();
            if (selectedEx != null) {
                try {
                    int series = Integer.parseInt(seriesField.getText().trim());
                    int rest = Integer.parseInt(restField.getText().trim());
                    String reps = repsField.getText().trim();
                    routineItems.add(new RoutineExercise(0, 0, selectedEx.name(), series, reps, rest, ""));
                    itemsModel.addRow(new Object[]{selectedEx.name(), series, reps, rest + " s", ""});
                } catch (Exception ex) {
                    alert("Verifica series y segundos de descanso.");
                }
            }
        });

        addExercisePanel.add(new JLabel("Ejercicio:"));
        addExercisePanel.add(exCombo);
        addExercisePanel.add(new JLabel("Series:"));
        addExercisePanel.add(seriesField);
        addExercisePanel.add(new JLabel("Reps:"));
        addExercisePanel.add(repsField);
        addExercisePanel.add(new JLabel("Descanso:"));
        addExercisePanel.add(restField);
        addExercisePanel.add(addExBtn);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        centerPanel.add(addExercisePanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = primaryButton("Guardar rutina");
        JButton cancelBtn = secondaryButton("Cancelar");

        saveBtn.addActionListener(e -> {
            try {
                Member m = (Member) memberCombo.getSelectedItem();
                if (m == null) throw new IllegalArgumentException("Debe seleccionar un socio.");
                service.assignRoutine(
                        m.id(),
                        nameField.getText(),
                        (String) goalCombo.getSelectedItem(),
                        notesField.getText(),
                        routineItems
                );
                alert("¡Rutina asignada exitosamente!");
                dialog.dispose();
                showRoutines();
            } catch (Exception ex) {
                alert("Error al guardar: " + ex.getMessage());
            }
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        bottomPanel.add(cancelBtn);
        bottomPanel.add(saveBtn);

        dialog.add(formTop, BorderLayout.NORTH);
        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showRoutineDetailsDialog(Routine r) {
        JDialog dialog = new JDialog(this, "Detalle de Rutina - " + r.name(), true);
        dialog.setSize(680, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel info = new JPanel(new GridLayout(4, 1, 4, 4));
        info.setBorder(new EmptyBorder(12, 15, 10, 15));
        info.add(boldLabel("Rutina: " + r.name(), 16));
        info.add(new JLabel("Socio: " + r.memberName() + " · Asignada: " + r.createdAt()));
        info.add(new JLabel("Objetivo: " + r.goal()));
        info.add(new JLabel("Notas: " + (r.notes().isBlank() ? "Sin notas adicionales" : r.notes())));

        Object[][] rows;
        if (r.exercises() != null && !r.exercises().isEmpty()) {
            rows = new Object[r.exercises().size()][5];
            for (int i = 0; i < r.exercises().size(); i++) {
                RoutineExercise re = r.exercises().get(i);
                rows[i] = new Object[]{i + 1, re.exerciseName(), re.series(), re.repetitions(), re.restSeconds() + " s"};
            }
        } else {
            rows = new Object[][]{{1, "Sin desglose de series", "-", "-", r.notes()}};
        }

        JTable table = createTable(new String[]{"#", "Ejercicio", "Series", "Repeticiones", "Descanso"}, rows);

        JButton closeBtn = secondaryButton("Cerrar");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(closeBtn);

        dialog.add(info, BorderLayout.NORTH);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showExerciseCatalogDialog() {
        JDialog dialog = new JDialog(this, "Catálogo Maestro de Ejercicios", true);
        dialog.setSize(740, 460);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        Runnable refresh = () -> {
            List<Exercise> list = service.exercises();
            Object[][] rows = new Object[list.size()][4];
            for (int i = 0; i < list.size(); i++) {
                Exercise ex = list.get(i);
                rows[i] = new Object[]{ex.id(), ex.name(), ex.muscleGroup(), ex.description()};
            }

            JTable table = createTable(new String[]{"ID", "Nombre de Ejercicio", "Grupo Muscular", "Descripción"}, rows);

            dialog.getContentPane().removeAll();

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton addBtn = primaryButton("+ Nuevo Ejercicio");
            addBtn.addActionListener(e -> {
                JTextField name = new JTextField();
                JComboBox<String> muscle = new JComboBox<>(new String[]{"Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Core", "Cardio"});
                JTextField desc = new JTextField();
                JPanel form = createForm(new String[]{"Nombre", "Grupo Muscular", "Descripción"}, new JComponent[]{name, muscle, desc});
                if (JOptionPane.showConfirmDialog(dialog, form, "Nuevo Ejercicio", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    try {
                        service.saveExercise(name.getText(), (String) muscle.getSelectedItem(), desc.getText());
                        dialog.dispose();
                        showExerciseCatalogDialog();
                    } catch (Exception ex) {
                        alert(ex.getMessage());
                    }
                }
            });
            top.add(addBtn);

            dialog.add(top, BorderLayout.NORTH);
            dialog.add(new JScrollPane(table), BorderLayout.CENTER);
            dialog.revalidate();
            dialog.repaint();
        };

        refresh.run();
        dialog.setVisible(true);
    }

    // ==========================================
    // 6. MÓDULO DE ASISTENCIA INTELIGENTE
    // ==========================================

    private void showAttendance() {
        List<Attendance> list = service.attendance();
        Object[][] rows = new Object[list.size()][6];
        for (int i = 0; i < list.size(); i++) {
            Attendance a = list.get(i);
            rows[i] = new Object[]{
                    a.id(), a.memberDocument(), a.memberName(), a.planName(),
                    a.statusAtCheckIn(), a.checkDate()
            };
        }

        JTable table = createTable(
                new String[]{"ID", "DNI/Documento", "Socio", "Plan", "Estado al Ingresar", "Fecha y Hora"},
                rows
        );

        JButton addBtn = primaryButton("+ Marcar ingreso (Control de acceso)");
        addBtn.addActionListener(e -> showAttendanceDialog());

        JButton filterBtn = secondaryButton("Filtrar por socio seleccionado");
        filterBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String doc = (String) table.getValueAt(row, 1);
                service.findMemberByDocument(doc).ifPresent(m -> {
                    List<Attendance> history = service.attendanceByMember(m.id());
                    alert("El socio " + m.fullName() + " tiene " + history.size() + " ingresos registrados en total.");
                });
            } else {
                alert("Selecciona un registro de asistencia.");
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(filterBtn);

        JPanel card = tableCard(table, addBtn);
        card.add(buttons, BorderLayout.SOUTH);

        page("Control de Asistencia", "Registro de ingresos con verificación automática de vigencia de membresía", card);
    }

    private void showAttendanceDialog() {
        List<Member> members = service.members();
        if (members.isEmpty()) {
            alert("No hay socios registrados.");
            return;
        }

        JComboBox<Member> memberCombo = new JComboBox<>(members.toArray(Member[]::new));
        memberCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Member m) {
                    setText(m.fullName() + " (DNI: " + m.document() + ") - Estado: " + m.status() + " [Vence: " + m.endDate() + "]");
                }
                return this;
            }
        });

        JTextField searchDocField = new JTextField(12);
        JButton searchBtn = secondaryButton("Buscar DNI");
        searchBtn.addActionListener(e -> {
            String query = searchDocField.getText().trim();
            if (!query.isBlank()) {
                for (int i = 0; i < memberCombo.getItemCount(); i++) {
                    Member m = memberCombo.getItemAt(i);
                    if (m.document().equalsIgnoreCase(query) || m.fullName().toLowerCase().contains(query.toLowerCase())) {
                        memberCombo.setSelectedIndex(i);
                        return;
                    }
                }
                alert("No se encontró ningún socio con el criterio ingresado.");
            }
        });

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Búsqueda rápida por DNI o Nombre:"));
        searchPanel.add(searchDocField);
        searchPanel.add(searchBtn);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.add(memberCombo, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this, mainPanel, "Registrar ingreso a las instalaciones", JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                Member selected = (Member) memberCombo.getSelectedItem();
                if (selected == null) throw new IllegalArgumentException("Debe seleccionar un socio.");

                CheckInResult checkInResult = service.checkIn(selected.id());

                if (checkInResult.isExpiredOrInactive()) {
                    JOptionPane.showMessageDialog(
                            this,
                            checkInResult.message(),
                            "ALERTA DE ACCESO - GYMDESK",
                            JOptionPane.WARNING_MESSAGE
                    );
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            checkInResult.message(),
                            "Acceso Permitido",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
                showAttendance();
            } catch (Exception e) {
                alert("Error al marcar asistencia: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 7. MÓDULO DE FACTURACIÓN (COMPROBANTES INTERNOS Y PDF)
    // ==========================================

    private void showInvoices() {
        GymConfig cfg = service.config();
        List<Payment> list = service.payments();
        Object[][] rows = new Object[list.size()][7];
        for (int i = 0; i < list.size(); i++) {
            Payment p = list.get(i);
            rows[i] = new Object[]{
                    p.invoiceNumber(), p.memberName(),
                    String.format(Locale.US, "%s %.2f", cfg.currency(), p.originalAmount()),
                    String.format(Locale.US, "%s %.2f", cfg.currency(), p.discountApplied()),
                    String.format(Locale.US, "%s %.2f", cfg.currency(), p.amount()),
                    p.date(), p.status()
            };
        }

        JTable table = createTable(new String[]{"N.º interno", "Cliente", "Precio Base", "Descuento", "Total Pagado", "Fecha", "Estado"}, rows);

        JButton noteBtn = secondaryButton("Ver nota en pantalla");
        noteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                alert("Selecciona un comprobante de la lista.");
                return;
            }
            String num = (String) table.getValueAt(row, 0);
            Payment p = service.payments().stream().filter(x -> x.invoiceNumber().equals(num)).findFirst().orElse(null);
            if (p == null) return;

            String info = cfg.gymName().toUpperCase() + " · COMPROBANTE INTERNO DE CAJA\n" +
                    "--------------------------------------------------\n" +
                    "Número de control: " + p.invoiceNumber() + "\n" +
                    "Cliente: " + p.memberName() + "\n" +
                    "Fecha de emisión: " + p.date() + "\n" +
                    "Concepto: " + p.concept() + "\n" +
                    "Método de pago: " + p.method() + "\n" +
                    "--------------------------------------------------\n" +
                    "Precio regular base: " + cfg.currency() + " " + String.format(Locale.US, "%.2f", p.originalAmount()) + "\n" +
                    "Promoción aplicada: " + (p.promotionName().isBlank() ? "Ninguna" : p.promotionName()) + "\n" +
                    "Descuento concedido: " + cfg.currency() + " " + String.format(Locale.US, "%.2f", p.discountApplied()) + "\n" +
                    "TOTAL PAGADO: " + cfg.currency() + " " + String.format(Locale.US, "%.2f", p.amount()) + "\n" +
                    "--------------------------------------------------\n" +
                    "Estado: " + p.status() + (p.isCancelled() ? (" (Motivo: " + p.cancellationReason() + ")") : "") + "\n\n" +
                    "Nota: " + cfg.receiptFooter();

            JOptionPane.showMessageDialog(this, info, "Detalle de Comprobante", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton pdfBtn = primaryButton("Exportar Recibo a PDF");
        pdfBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                alert("Selecciona un comprobante de la tabla para exportar a PDF.");
                return;
            }
            String num = (String) table.getValueAt(row, 0);
            Payment p = service.payments().stream().filter(x -> x.invoiceNumber().equals(num)).findFirst().orElse(null);
            if (p == null) return;

            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("Recibo_" + p.invoiceNumber() + ".pdf"));
            chooser.setFileFilter(new FileNameExtensionFilter("Documentos PDF (*.pdf)", "pdf"));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".pdf")) {
                    file = new File(file.getAbsolutePath() + ".pdf");
                }
                try {
                    service.exportReceiptPdf(p, file);
                    int op = JOptionPane.showConfirmDialog(
                            this,
                            "Recibo PDF exportado exitosamente en:\n" + file.getAbsolutePath() + "\n\n¿Deseas abrir el archivo ahora?",
                            "PDF Generado",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (op == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    }
                } catch (Exception ex) {
                    alert("Error al exportar PDF: " + ex.getMessage());
                }
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(noteBtn);
        buttons.add(pdfBtn);

        JPanel card = tableCard(table, null);
        card.add(buttons, BorderLayout.SOUTH);

        page("Facturación y comprobantes", "Consulta de notas internas y exportación oficial a PDF para clientes", card);
    }

    // ==========================================
    // 8. MÓDULO DE REPORTES AVANZADOS
    // ==========================================

    private void showReports() {
        GymConfig cfg = service.config();
        JPanel body = panel();
        body.setLayout(new BorderLayout(0, 14));

        // Filtro por fechas
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterBar.setOpaque(false);

        LocalDate now = LocalDate.now();
        JTextField startField = new JTextField(now.withDayOfMonth(1).toString(), 8);
        JTextField endField = new JTextField(now.toString(), 8);
        JButton applyBtn = primaryButton("Generar reporte");

        JButton todayBtn = secondaryButton("Hoy");
        todayBtn.addActionListener(e -> {
            startField.setText(now.toString());
            endField.setText(now.toString());
            applyBtn.doClick();
        });

        JButton monthBtn = secondaryButton("Este mes");
        monthBtn.addActionListener(e -> {
            startField.setText(now.withDayOfMonth(1).toString());
            endField.setText(now.toString());
            applyBtn.doClick();
        });

        filterBar.add(new JLabel("Desde (YYYY-MM-DD):"));
        filterBar.add(startField);
        filterBar.add(new JLabel("Hasta:"));
        filterBar.add(endField);
        filterBar.add(applyBtn);
        filterBar.add(todayBtn);
        filterBar.add(monthBtn);

        // Panel central dinámico
        JPanel dynamicContainer = new JPanel(new BorderLayout(0, 12));
        dynamicContainer.setOpaque(false);

        Runnable loadReport = () -> {
            try {
                LocalDate start = LocalDate.parse(startField.getText().trim());
                LocalDate end = LocalDate.parse(endField.getText().trim());
                ReportSummary rep = service.generateReport(start, end);

                dynamicContainer.removeAll();

                // Tarjetas KPI del período
                JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 12, 0));
                kpiPanel.setOpaque(false);
                kpiPanel.add(createStatCard("INGRESOS NETOS", String.format(Locale.US, "%s %.2f", cfg.currency(), rep.grossIncome()), "Cobros efectivos del período"));
                kpiPanel.add(createStatCard("OPERACIONES", rep.activePaymentsCount(), "Cobros vigentes"));
                kpiPanel.add(createStatCard("DESCUENTOS OTORGADOS", String.format(Locale.US, "%s %.2f", cfg.currency(), rep.totalDiscounts()), "Ahorro total concedido"));
                kpiPanel.add(createStatCard("ASISTENCIAS", rep.totalAttendancesCount(), "Ingresos registrados"));

                // Tabla detallada
                Object[][] rows = new Object[rep.payments().size()][7];
                for (int i = 0; i < rep.payments().size(); i++) {
                    Payment p = rep.payments().get(i);
                    rows[i] = new Object[]{
                            p.invoiceNumber(), p.memberName(), p.concept(),
                            String.format(Locale.US, "%.2f", p.originalAmount()),
                            String.format(Locale.US, "%.2f", p.discountApplied()),
                            String.format(Locale.US, "%.2f", p.amount()),
                            p.status()
                    };
                }

                JTable repTable = createTable(new String[]{"N.º Interno", "Socio", "Concepto", "Base", "Descuento", "Total", "Estado"}, rows);

                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                actions.setOpaque(false);

                JButton pdfReportBtn = primaryButton("Exportar Reporte a PDF");
                pdfReportBtn.addActionListener(e -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setSelectedFile(new File("Reporte_Caja_" + rep.startDate() + "_" + rep.endDate() + ".pdf"));
                    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();
                        if (!file.getName().toLowerCase().endsWith(".pdf")) {
                            file = new File(file.getAbsolutePath() + ".pdf");
                        }
                        try {
                            service.exportReportPdf(rep, file);
                            int op = JOptionPane.showConfirmDialog(
                                    this,
                                    "Reporte PDF exportado exitosamente en:\n" + file.getAbsolutePath() + "\n\n¿Deseas abrir el archivo ahora?",
                                    "Reporte PDF",
                                    JOptionPane.YES_NO_OPTION
                            );
                            if (op == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().open(file);
                            }
                        } catch (Exception ex) {
                            alert("Error al exportar: " + ex.getMessage());
                        }
                    }
                });

                JButton expiringBtn = secondaryButton("Socios por vencer (Próximos 7 días)");
                expiringBtn.addActionListener(e -> showExpiringMembersDialog());

                actions.add(expiringBtn);
                actions.add(pdfReportBtn);

                dynamicContainer.add(kpiPanel, BorderLayout.NORTH);
                dynamicContainer.add(new JScrollPane(repTable), BorderLayout.CENTER);
                dynamicContainer.add(actions, BorderLayout.SOUTH);

                dynamicContainer.revalidate();
                dynamicContainer.repaint();

            } catch (Exception ex) {
                alert("Error al calcular reporte: " + ex.getMessage());
            }
        };

        applyBtn.addActionListener(e -> loadReport.run());

        body.add(filterBar, BorderLayout.NORTH);
        body.add(dynamicContainer, BorderLayout.CENTER);

        loadReport.run();

        page("Reportes e Indicadores", "Análisis financiero, consolidado de caja y control de vencimientos", body);
    }

    private void showExpiringMembersDialog() {
        List<Member> list = service.expiringMembers(7);
        Object[][] rows = new Object[list.size()][5];
        for (int i = 0; i < list.size(); i++) {
            Member m = list.get(i);
            rows[i] = new Object[]{m.fullName(), m.document(), m.phone(), m.plan(), m.endDate()};
        }

        JTable table = createTable(new String[]{"Socio", "Documento", "Teléfono", "Plan", "Fecha Vencimiento"}, rows);

        JDialog dialog = new JDialog(this, "Socios con membresía por vencer (Próximos 7 días)", true);
        dialog.setSize(680, 380);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBorder(new EmptyBorder(12, 12, 12, 12));
        p.add(new JLabel("Socios activos con vencimiento próximo para contacto y renovación:"), BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton close = secondaryButton("Cerrar");
        close.addActionListener(e -> dialog.dispose());
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bot.add(close);
        p.add(bot, BorderLayout.SOUTH);

        dialog.add(p);
        dialog.setVisible(true);
    }

    // ==========================================
    // 9. MÓDULO DE CONFIGURACIÓN COMERCIAL
    // ==========================================

    private void showConfig() {
        GymConfig current = service.config();

        JTextField nameField = new JTextField(current.gymName());
        JTextField taxField = new JTextField(current.taxId());
        JTextField addrField = new JTextField(current.address());
        JTextField phoneField = new JTextField(current.phone());
        JTextField emailField = new JTextField(current.email());
        JTextField currencyField = new JTextField(current.currency());
        JTextField footerField = new JTextField(current.receiptFooter());

        JPanel form = createForm(
                new String[]{
                        "Nombre comercial del gimnasio", "RUC / DNI comercial",
                        "Dirección comercial", "Teléfono de contacto",
                        "Correo electrónico", "Símbolo de Moneda (ej. S/, $, €)",
                        "Leyenda para comprobantes"
                },
                new JComponent[]{nameField, taxField, addrField, phoneField, emailField, currencyField, footerField}
        );

        JPanel card = panel();
        card.add(form, BorderLayout.CENTER);

        JButton saveBtn = primaryButton("Guardar cambios de configuración");
        saveBtn.addActionListener(e -> {
            try {
                GymConfig newConfig = new GymConfig(
                        nameField.getText().trim(),
                        taxField.getText().trim(),
                        addrField.getText().trim(),
                        phoneField.getText().trim(),
                        emailField.getText().trim(),
                        currencyField.getText().trim(),
                        footerField.getText().trim()
                );
                service.saveConfig(newConfig);
                setTitle("GymDesk | " + newConfig.gymName());
                alert("¡Configuración actualizada con éxito!\nLos nuevos comprobantes reflejarán estos datos.");
                showConfig();
            } catch (Exception ex) {
                alert("Error al guardar: " + ex.getMessage());
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(saveBtn);
        card.add(bottom, BorderLayout.SOUTH);

        page("Configuración comercial", "Personaliza los datos de tu gimnasio para recibos, reportes y moneda", card);
    }

    // ==========================================
    // 10. MÓDULO DE USUARIOS Y ROLES (RBAC)
    // ==========================================

    private void showUsers() {
        if (!service.isAdmin()) {
            alert("Acceso denegado: Solo el Administrador puede gestionar usuarios.");
            showDashboard();
            return;
        }

        List<User> list = service.users();
        Object[][] rows = new Object[list.size()][6];
        for (int i = 0; i < list.size(); i++) {
            User u = list.get(i);
            rows[i] = new Object[]{
                    u.id(), u.username(), u.fullName(), u.role(),
                    u.active() ? "Activo" : "Inactivo", u.createdAt()
            };
        }

        JTable table = createTable(
                new String[]{"ID", "Usuario", "Nombre Completo", "Rol", "Estado", "Fecha Registro"},
                rows
        );

        JButton addBtn = primaryButton("+ Nuevo usuario");
        addBtn.addActionListener(e -> showNewUserDialog());

        JButton toggleBtn = secondaryButton("Activar / Desactivar seleccionado");
        toggleBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                alert("Selecciona un usuario de la tabla.");
                return;
            }
            int id = (int) table.getValueAt(row, 0);
            String username = (String) table.getValueAt(row, 1);
            boolean isActive = "Activo".equals(table.getValueAt(row, 4));

            String action = isActive ? "desactivar" : "activar";
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "¿Desea " + action + " la cuenta de usuario '" + username + "'?",
                    "Confirmar cambio de estado",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    service.toggleUserStatus(id, !isActive);
                    alert("Estado de usuario actualizado correctamente.");
                    showUsers();
                } catch (Exception ex) {
                    alert("Error: " + ex.getMessage());
                }
            }
        });

        JButton passBtn = secondaryButton("Cambiar contraseña");
        passBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                alert("Selecciona un usuario de la tabla.");
                return;
            }
            int id = (int) table.getValueAt(row, 0);
            String username = (String) table.getValueAt(row, 1);
            showChangePasswordDialog(id, username);
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(passBtn);
        buttons.add(toggleBtn);

        JPanel card = tableCard(table, addBtn);
        card.add(buttons, BorderLayout.SOUTH);

        page("Usuarios y Permisos", "Gestión de cuentas del personal, roles administrativos y seguridad criptográfica", card);
    }

    private void showNewUserDialog() {
        JTextField usernameField = new JTextField();
        JTextField nameField = new JTextField();
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{User.ROLE_ADMIN, User.ROLE_RECEPTIONIST});
        JPasswordField passField = new JPasswordField();

        JPanel form = createForm(
                new String[]{"Nombre de usuario (ej. ctrainer)", "Nombre completo", "Rol asignado", "Contraseña inicial (mín. 6 caracteres)"},
                new JComponent[]{usernameField, nameField, roleCombo, passField}
        );

        int result = JOptionPane.showConfirmDialog(this, form, "Crear nuevo usuario", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String username = usernameField.getText().trim();
                String name = nameField.getText().trim();
                String role = (String) roleCombo.getSelectedItem();
                String pass = new String(passField.getPassword());

                service.createUser(username, pass, name, role);
                alert("¡Usuario '" + username + "' creado exitosamente!");
                showUsers();
            } catch (Exception ex) {
                alert("Error al crear usuario: " + ex.getMessage());
            }
        }
    }

    private void showChangePasswordDialog(int userId, String username) {
        JPasswordField newPassField = new JPasswordField();
        JPasswordField repeatPassField = new JPasswordField();

        JPanel form = createForm(
                new String[]{"Nueva contraseña (mín. 6 caracteres)", "Confirmar nueva contraseña"},
                new JComponent[]{newPassField, repeatPassField}
        );

        int result = JOptionPane.showConfirmDialog(
                this, form, "Cambiar contraseña de '" + username + "'", JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            String p1 = new String(newPassField.getPassword());
            String p2 = new String(repeatPassField.getPassword());

            if (!p1.equals(p2)) {
                alert("Las contraseñas ingresadas no coinciden.");
                return;
            }
            try {
                service.changePassword(userId, null, p1);
                alert("Contraseña actualizada con éxito para el usuario '" + username + "'.");
            } catch (Exception ex) {
                alert("Error: " + ex.getMessage());
            }
        }
    }

    // ==========================================
    // 11. MÓDULO DE COPIAS DE SEGURIDAD (RESPALDOS)
    // ==========================================

    private void showBackups() {
        if (!service.isAdmin()) {
            alert("Acceso denegado: Solo el Administrador puede gestionar respaldos.");
            showDashboard();
            return;
        }

        JPanel body = panel();
        body.setLayout(new BorderLayout(0, 20));

        // Panel de información de estado
        JPanel statusGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        statusGrid.setOpaque(false);

        statusGrid.add(createStatCard("MOTOR DE BASE DE DATOS", "SQLite 3", "Modo archivo local"));
        statusGrid.add(createStatCard("MECANISMO DE COPIA", "Snapshot Online", "Comando SQLite VACUUM INTO"));
        statusGrid.add(createStatCard("ÚLTIMO RESPALDO", service.getLastBackupDate(), "Fecha/hora de la última copia"));

        // Panel central con descripción e instrucciones
        JPanel centerCard = new JPanel(new BorderLayout(0, 14));
        centerCard.setBackground(Color.WHITE);
        centerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 232, 240)),
                new EmptyBorder(22, 24, 22, 24)
        ));

        JLabel infoTitle = new JLabel("Protección y Salvaguarda de la Información");
        infoTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        infoTitle.setForeground(navy);

        JTextArea infoText = new JTextArea(
                "GymDesk utiliza SQLite como motor de persistencia local. Para prevenir pérdidas por fallas en el disco o errores del sistema operativo:\n\n" +
                "• Respaldo en caliente: Genera una copia consistente completa de la base de datos sin necesidad de cerrar la aplicación.\n" +
                "• Destino recomendado: Guarde periódicamente las copias en una memoria USB, disco externo o almacenamiento seguro.\n" +
                "• Restauración: Permite cargar un archivo .db previamente respaldado. Se crea un respaldo de emergencia antes de sobreescribir."
        );
        infoText.setFont(new Font("SansSerif", Font.PLAIN, 13));
        infoText.setForeground(new Color(60, 75, 95));
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        infoText.setEditable(false);
        infoText.setOpaque(false);

        // Botones de acción
        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        actionButtons.setOpaque(false);

        JButton createBackupBtn = primaryButton("💾 Crear Copia de Seguridad ahora");
        createBackupBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Seleccione la carpeta de destino para el archivo de respaldo");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File targetDir = chooser.getSelectedFile();
                try {
                    String backupFilePath = service.createBackup(targetDir);
                    JOptionPane.showMessageDialog(
                            this,
                            "¡Copia de seguridad creada exitosamente!\n\nArchivo generado:\n" + backupFilePath +
                            "\n\nGuarde este archivo en un lugar seguro para cualquier contingencia.",
                            "Respaldo Exitoso",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    showBackups();
                } catch (Exception ex) {
                    alert("Error al generar copia de seguridad: " + ex.getMessage());
                }
            }
        });

        JButton restoreBackupBtn = dangerButton("↺ Restaurar Copia de Seguridad desde Archivo");
        restoreBackupBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Seleccione el archivo de base de datos de respaldo (.db)");
            chooser.setFileFilter(new FileNameExtensionFilter("Bases de datos SQLite (*.db)", "db"));

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File backupFile = chooser.getSelectedFile();

                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "⚠️ ADVERTENCIA CRÍTICA:\n\n" +
                        "Restaurar el respaldo reemplazará los datos actuales por los del archivo:\n" +
                        backupFile.getName() + "\n\n" +
                        "Se creará automáticamente una copia de resguardo de emergencia previa (.pre_restore.bak).\n" +
                        "¿Desea continuar y recargar el sistema con esta copia?",
                        "Confirmar Restauración de Base de Datos",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        service.restoreBackup(backupFile);
                        JOptionPane.showMessageDialog(
                                this,
                                "¡La base de datos fue restaurada exitosamente!\nSe actualizará la vista del sistema.",
                                "Restauración Completada",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        showDashboard();
                    } catch (Exception ex) {
                        alert("Error al restaurar: " + ex.getMessage());
                    }
                }
            }
        });

        actionButtons.add(createBackupBtn);
        actionButtons.add(restoreBackupBtn);

        centerCard.add(infoTitle, BorderLayout.NORTH);
        centerCard.add(infoText, BorderLayout.CENTER);
        centerCard.add(actionButtons, BorderLayout.SOUTH);

        body.add(statusGrid, BorderLayout.NORTH);
        body.add(centerCard, BorderLayout.CENTER);

        page("Copias de Seguridad (Respaldos)", "Protección y restauración de la base de datos de GymDesk", body);
    }

    // ==========================================
    // 12. MÓDULO DE AUDITORÍA
    // ==========================================

    private void showAuditLogs() {
        if (!service.isAdmin()) {
            alert("Acceso denegado: Solo el Administrador puede consultar los registros de auditoría.");
            showDashboard();
            return;
        }

        List<AuditLog> list = service.recentAuditLogs(300);
        Object[][] rows = new Object[list.size()][5];
        for (int i = 0; i < list.size(); i++) {
            AuditLog a = list.get(i);
            rows[i] = new Object[]{
                    a.id(), a.timestamp(), a.username(), a.action(), a.details()
            };
        }

        JTable table = createTable(
                new String[]{"ID", "Fecha y Hora", "Usuario", "Acción Realizada", "Detalle de Auditoría"},
                rows
        );

        JPanel card = tableCard(table, null);

        JLabel note = new JLabel("  * Registro cronológico inmutable de seguridad y control interno. Cumple con trazabilidad de operaciones.");
        note.setForeground(Color.GRAY);
        note.setFont(new Font("SansSerif", Font.ITALIC, 11));
        note.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.add(note, BorderLayout.SOUTH);

        page("Registro de Auditoría", "Trazabilidad de seguridad: accesos, cobros, anulaciones, membresías y respaldos", card);
    }

    private void showMemberProfileDialog(Member member) {
        GymConfig cfg = service.config();
        JDialog dialog = new JDialog(this, "Ficha Integral del Socio - " + member.fullName(), true);
        dialog.setSize(820, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();

        // Pestaña 1: Resumen General
        JPanel infoPanel = panel();
        infoPanel.setLayout(new GridLayout(4, 2, 14, 12));
        infoPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        infoPanel.add(new JLabel("<html><b>Nombre Completo:</b> " + member.fullName() + "</html>"));
        infoPanel.add(new JLabel("<html><b>N.º Documento:</b> " + member.document() + "</html>"));
        infoPanel.add(new JLabel("<html><b>Teléfono:</b> " + member.phone() + "</html>"));
        infoPanel.add(new JLabel("<html><b>Correo:</b> " + member.email() + "</html>"));
        infoPanel.add(new JLabel("<html><b>Plan Actual:</b> " + member.plan() + "</html>"));
        infoPanel.add(new JLabel("<html><b>Estado:</b> " + member.status() + "</html>"));
        infoPanel.add(new JLabel("<html><b>Fecha Inicio:</b> " + member.startDate() + "</html>"));
        infoPanel.add(new JLabel("<html><b>Fecha Fin / Vencimiento:</b> " + member.endDate() + "</html>"));

        tabs.addTab("Datos Generales", infoPanel);

        // Pestaña 2: Membresías
        List<Membership> mList = service.membershipsByMember(member.id());
        Object[][] mRows = new Object[mList.size()][5];
        for (int i = 0; i < mList.size(); i++) {
            Membership ms = mList.get(i);
            mRows[i] = new Object[]{ms.id(), ms.planName(), ms.startDate(), ms.endDate(), ms.status()};
        }
        JTable mTable = createTable(new String[]{"ID", "Plan", "Inicio", "Fin", "Estado"}, mRows);
        tabs.addTab("Membresías (" + mList.size() + ")", new JScrollPane(mTable));

        // Pestaña 3: Pagos / Comprobantes
        List<Payment> pList = service.payments().stream().filter(p -> p.memberId() == member.id()).toList();
        Object[][] pRows = new Object[pList.size()][6];
        for (int i = 0; i < pList.size(); i++) {
            Payment p = pList.get(i);
            pRows[i] = new Object[]{
                    p.invoiceNumber(), p.concept(),
                    String.format(Locale.US, "%s %.2f", cfg.currency(), p.amount()),
                    p.method(), p.date(), p.status()
            };
        }
        JTable pTable = createTable(new String[]{"Comprobante", "Concepto", "Monto", "Método", "Fecha", "Estado"}, pRows);
        tabs.addTab("Cobros (" + pList.size() + ")", new JScrollPane(pTable));

        // Pestaña 4: Asistencias
        List<Attendance> aList = service.attendanceByMember(member.id());
        Object[][] aRows = new Object[aList.size()][3];
        for (int i = 0; i < aList.size(); i++) {
            Attendance a = aList.get(i);
            aRows[i] = new Object[]{a.id(), a.checkDate(), a.statusAtCheckIn()};
        }
        JTable aTable = createTable(new String[]{"ID", "Fecha y Hora de Ingreso", "Estado al Marcar"}, aRows);
        tabs.addTab("Asistencias (" + aList.size() + ")", new JScrollPane(aTable));

        // Pestaña 5: Rutinas
        List<Routine> rList = service.routinesByMember(member.id());
        Object[][] rRows = new Object[rList.size()][4];
        for (int i = 0; i < rList.size(); i++) {
            Routine r = rList.get(i);
            rRows[i] = new Object[]{r.id(), r.name(), r.goal(), r.createdAt()};
        }
        JTable rTable = createTable(new String[]{"ID", "Nombre de Rutina", "Objetivo", "Fecha Creación"}, rRows);
        tabs.addTab("Rutinas (" + rList.size() + ")", new JScrollPane(rTable));

        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setBorder(new EmptyBorder(12, 12, 12, 12));
        container.add(tabs, BorderLayout.CENTER);

        JButton closeBtn = secondaryButton("Cerrar");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bot.add(closeBtn);
        container.add(bot, BorderLayout.SOUTH);

        dialog.add(container);
        dialog.setVisible(true);
    }
}
