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
import com.gymdesk.domain.RoutineExercise;
import com.gymdesk.domain.User;
import com.gymdesk.security.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteGymRepositoryTest {

    @TempDir
    Path tempDir;

    private SqliteGymRepository repository;
    private File dbFile;

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("test_gymdesk.db").toFile();
        repository = new SqliteGymRepository("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    @DisplayName("Debe inicializar la base de datos con planes, socios, membresías, ofertas, ejercicios y configuración")
    void shouldInitializeWithSeedData() {
        List<Plan> plans = repository.plans();
        assertFalse(plans.isEmpty());
        assertEquals(3, plans.size());

        List<Member> members = repository.members();
        assertFalse(members.isEmpty());
        assertEquals(3, members.size());

        assertTrue(repository.count("memberships") >= 3);
        assertTrue(repository.count("offers") >= 3);
        assertTrue(repository.count("exercises") >= 10);

        GymConfig config = repository.config();
        assertNotNull(config);
        assertEquals("GymDesk Fitness Club", config.gymName());
    }

    @Test
    @DisplayName("saveConfig y config deben almacenar y recuperar la configuración personalizada")
    void shouldSaveAndRetrieveCustomConfig() {
        GymConfig custom = new GymConfig("Iron Gym Perú", "20556677889", "Av. Grau 450", "955444333", "info@irongym.pe", "$", "Gracias por su visita.");
        repository.saveConfig(custom);

        GymConfig fetched = repository.config();
        assertEquals("Iron Gym Perú", fetched.gymName());
        assertEquals("20556677889", fetched.taxId());
        assertEquals("$", fetched.currency());
    }

    @Test
    @DisplayName("paymentsBetween y countAttendanceBetween filtran operaciones por rango de fechas")
    void shouldFilterOperationsByDateRange() {
        List<Member> members = repository.members();
        Member member = members.get(0);

        repository.addPayment(member.id(), 120.0, 0.0, 120.0, "Ninguna", "Efectivo", "Mensualidad");
        repository.checkIn(member.id());

        String today = LocalDate.now().toString();
        List<Payment> rangePayments = repository.paymentsBetween(today, today);
        assertFalse(rangePayments.isEmpty());
        assertEquals(120.0, rangePayments.get(0).amount());

        int countAtt = repository.countAttendanceBetween(today, today);
        assertTrue(countAtt >= 1);
    }

    @Test
    @DisplayName("saveMember debe insertar y actualizar socios correctamente")
    void shouldInsertAndRetrieveMember() {
        Member newMember = new Member(
                0, "44556677", "Carlos Santana", "987654321", "carlos@test.com",
                "Activo", "Mensual", "2026-11-01", "2026-12-01"
        );
        repository.saveMember(newMember);

        List<Member> all = repository.members();
        Member found = all.stream()
                .filter(m -> "44556677".equals(m.document()))
                .findFirst()
                .orElse(null);

        assertNotNull(found);
        assertEquals("Carlos Santana", found.fullName());
    }

    @Test
    @DisplayName("deactivateMember debe cambiar el estado del socio a Inactivo")
    void shouldDeactivateMember() {
        List<Member> initial = repository.members();
        Member target = initial.get(0);

        repository.deactivateMember(target.id());

        Member updated = repository.findMemberById(target.id()).orElseThrow();
        assertEquals("Inactivo", updated.status());
    }

    @Test
    @DisplayName("addPayment debe registrar un pago con desglose de promoción y anularlo con motivo")
    void shouldAddAndCancelPaymentWithAudit() {
        List<Member> members = repository.members();
        Member member = members.get(0);

        int initialCount = repository.count("payments");
        repository.addPayment(member.id(), 100.0, 15.0, 85.0, "Descuento Apertura", "Efectivo", "Mensualidad");

        assertEquals(initialCount + 1, repository.count("payments"));
        List<Payment> payments = repository.payments();
        Payment p = payments.get(0);
        assertEquals(100.0, p.originalAmount());
        assertEquals(15.0, p.discountApplied());
        assertEquals(85.0, p.amount());
        assertEquals("Descuento Apertura", p.promotionName());
        assertEquals("Registrado", p.status());

        repository.cancelPayment(p.id(), "Error en comprobante de pago");

        Payment cancelled = repository.findPaymentById(p.id()).orElseThrow();
        assertEquals("Anulado", cancelled.status());
        assertEquals("Error en comprobante de pago", cancelled.cancellationReason());
    }

    @Test
    @DisplayName("saveRoutine y routinesByMember guardan rutinas estructuradas con ejercicios")
    void shouldSaveAndRetrieveStructuredRoutine() {
        List<Member> members = repository.members();
        Member member = members.get(0);

        List<RoutineExercise> items = List.of(
                new RoutineExercise(0, 0, "Press de banca plano", 4, "10-12", 60, "RPE 8"),
                new RoutineExercise(0, 0, "Dominadas en barra fija", 3, "8", 90, "Pronas")
        );

        Routine routine = new Routine(0, member.id(), member.fullName(), "Full Body Fuerza", "Fuerza", "Semana 1", "2026-11-04", items);
        repository.saveRoutine(routine);

        List<Routine> routines = repository.routinesByMember(member.id());
        assertFalse(routines.isEmpty());
        Routine saved = routines.get(0);
        assertEquals("Full Body Fuerza", saved.name());
        assertEquals(2, saved.exercises().size());
    }

    @Test
    @DisplayName("Debe inicializar usuarios por defecto con hashes PBKDF2 válidos")
    void shouldInitializeWithDefaultUsers() {
        List<User> users = repository.users();
        assertTrue(users.size() >= 2);

        User admin = repository.findUserByUsername("admin").orElseThrow();
        assertEquals("ADMINISTRADOR", admin.role());
        assertTrue(admin.active());
        assertTrue(PasswordUtil.verifyPassword("admin123", admin.salt(), admin.passwordHash()));

        User recep = repository.findUserByUsername("recepcion").orElseThrow();
        assertEquals("RECEPCIONISTA", recep.role());
        assertTrue(recep.active());
        assertTrue(PasswordUtil.verifyPassword("recepcion123", recep.salt(), recep.passwordHash()));
    }

    @Test
    @DisplayName("saveUser y toggleUserStatus gestionan usuarios en SQLite")
    void shouldSaveAndToggleUserInSqlite() {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword("pass123", salt);
        User newUser = new User(0, "lucia", hash, salt, "Lucia Morales", User.ROLE_RECEPTIONIST, true, "2026-11-05");
        repository.saveUser(newUser);

        User found = repository.findUserByUsername("lucia").orElseThrow();
        assertEquals("Lucia Morales", found.fullName());
        assertTrue(found.active());

        repository.toggleUserStatus(found.id(), false);
        User toggled = repository.findUserById(found.id()).orElseThrow();
        assertFalse(toggled.active());
    }

    @Test
    @DisplayName("createBackup genera un snapshot válido mediante VACUUM INTO en SQLite")
    void shouldCreateAndVerifySqliteBackup() {
        File backupDir = tempDir.resolve("backups").toFile();
        backupDir.mkdirs();

        String backupPath = repository.createBackup(backupDir);
        assertNotNull(backupPath);

        File backupFile = new File(backupPath);
        assertTrue(backupFile.exists());
        assertTrue(backupFile.length() > 0, "El archivo de respaldo SQLite debe contener datos");

        assertNotEquals("Nunca", repository.getLastBackupDate());

        // Verificar que restoreBackup acepte el respaldo
        assertDoesNotThrow(() -> repository.restoreBackup(backupFile));
    }

    @Test
    @DisplayName("addAuditLog y recentAuditLogs almacenan y recuperan registros de auditoría en SQLite")
    void shouldPersistAndQueryAuditLogsInSqlite() {
        AuditLog log1 = new AuditLog(0, "2026-11-05 10:00:00", "admin", AuditLog.ACTION_LOGIN, "Inicio de sesión de prueba");
        AuditLog log2 = new AuditLog(0, "2026-11-05 10:05:00", "admin", AuditLog.ACTION_PAYMENT, "Cobro de S/ 80.00 registrado");

        repository.addAuditLog(log1);
        repository.addAuditLog(log2);

        List<AuditLog> logs = repository.recentAuditLogs(10);
        assertTrue(logs.size() >= 2);
        assertEquals(AuditLog.ACTION_PAYMENT, logs.get(0).action(), "Debe ordenar descendentemente por ID/fecha");
        assertEquals("admin", logs.get(0).username());
    }
}
