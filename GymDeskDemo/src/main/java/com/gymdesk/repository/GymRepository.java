package com.gymdesk.repository;

import com.gymdesk.domain.Attendance;
import com.gymdesk.domain.AuditLog;
import com.gymdesk.domain.Exercise;
import com.gymdesk.domain.GymConfig;
import com.gymdesk.domain.Member;
import com.gymdesk.domain.Membership;
import com.gymdesk.domain.Offer;
import com.gymdesk.domain.Payment;
import com.gymdesk.domain.Plan;
import com.gymdesk.domain.Routine;
import com.gymdesk.domain.User;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia para el sistema GymDesk.
 */
public interface GymRepository {

    // --- Socios ---
    List<Member> members();
    Optional<Member> findMemberById(int id);
    void saveMember(Member member);
    void deactivateMember(int id);
    void deleteMember(int id);
    void updateMemberPlanAndDates(int memberId, String planName, String startDate, String endDate, String status);

    // --- Planes ---
    List<Plan> plans();
    Optional<Plan> findPlanById(int id);
    void savePlan(Plan plan);
    void updatePlan(Plan plan);

    // --- Membresías (Historial y renovaciones) ---
    List<Membership> membershipsByMember(int memberId);
    Optional<Membership> findLatestMembership(int memberId);
    void saveMembership(Membership membership);

    // --- Ofertas y Promociones ---
    List<Offer> offers();
    Optional<Offer> findOfferById(int id);
    void saveOffer(Offer offer);
    void toggleOfferStatus(int id, boolean active);

    // --- Pagos / Cobros ---
    List<Payment> payments();
    List<Payment> paymentsBetween(String startDate, String endDate);
    Optional<Payment> findPaymentById(int id);
    void addPayment(int memberId, double originalAmount, double discountApplied, double finalAmount, String promotionName, String method, String concept);
    void cancelPayment(int paymentId, String reason);

    default void addPayment(int memberId, double amount, String method, String concept) {
        addPayment(memberId, amount, 0.0, amount, "Ninguna", method, concept);
    }

    // --- Catálogo de Ejercicios ---
    List<Exercise> exercises();
    void saveExercise(Exercise exercise);

    // --- Rutinas ---
    List<Routine> routines();
    List<Routine> routinesByMember(int memberId);
    void saveRoutine(Routine routine);

    // --- Asistencia ---
    List<Attendance> attendance();
    List<Attendance> attendanceByMember(int memberId);
    int countAttendanceBetween(String startDate, String endDate);
    Attendance checkIn(int memberId);

    // --- Configuración Comercial del Gimnasio ---
    GymConfig config();
    void saveConfig(GymConfig config);

    // --- Usuarios y Roles ---
    List<User> users();
    Optional<User> findUserById(int id);
    Optional<User> findUserByUsername(String username);
    void saveUser(User user);
    void updateUser(User user);
    void updateUserPassword(int userId, String newHash, String newSalt);
    void toggleUserStatus(int id, boolean active);

    // --- Copias de Seguridad (Respaldos) ---
    String createBackup(File targetDirectory);
    void restoreBackup(File backupFile);
    String getLastBackupDate();

    // --- Auditoría de Eventos del Sistema ---
    void addAuditLog(AuditLog log);
    List<AuditLog> auditLogs();
    List<AuditLog> recentAuditLogs(int limit);

    // --- Indicadores y Reportes ---
    int count(String table);
    double incomeThisMonth();
}
