package com.gymdesk.app;

import com.gymdesk.repository.GymRepository;
import com.gymdesk.repository.SqliteGymRepository;
import com.gymdesk.service.GymService;
import com.gymdesk.ui.LoginDialog;
import com.gymdesk.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada principal para la aplicación de escritorio GymDesk.
 */
public class GymDeskApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Aplicar Look & Feel del sistema operativo para una apariencia nativa
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Si falla el LookAndFeel del sistema, continúa con el por defecto de Swing
            }

            GymRepository repository = new SqliteGymRepository();
            GymService service = new GymService(repository);

            startApplication(service);
        });
    }

    public static void startApplication(GymService service) {
        LoginDialog loginDialog = new LoginDialog(null, service);
        loginDialog.setVisible(true);

        if (loginDialog.isAuthenticated()) {
            MainFrame mainFrame = new MainFrame(service);
            mainFrame.setVisible(true);
        } else {
            System.exit(0);
        }
    }
}
