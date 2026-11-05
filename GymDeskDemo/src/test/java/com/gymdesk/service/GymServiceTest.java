package com.gymdesk.service;

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
import com.gymdesk.repository.GymRepository;
import com.gymdesk.security.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GymServiceTest {

    @TempDir
    Path tempDir;

    private InMemoryGymRepository repository;
    private GymService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGymRepository();
        service = new GymService(repository);
        service.authenticate("admin", "admin123");
    }

    @Test
    @DisplayName("saveMember debe rechazar documento o nombre vacíos")
    void shouldRejectInvalidMemberData() {
        Member emptyDoc = new Member(0, "", "Juan Perez", "999888777", "juan@test.com", "Activo", "Mensual", "2026-11-01", "2026-12-01");
        assertThrows(IllegalArgumentException.class, () -> service.saveMember(emptyDoc));

        Member emptyName = new Member(0, "12345678", "   ", "999888777", "juan@test.com", "Activo", "Mensual", "2026-11-01", "2026-12-01");
        assertThrows(IllegalArgumentException.class, () -> service.saveMember(emptyName));
    }

    @Test
    @DisplayName("saveMember guarda un socio correctamente cuando los datos son válidos")
    void shouldSaveValidMember() {
        Member valid = new Member(0, "12345678", "Juan Perez", "999888777", "juan@test.com", "Activo", "Mensual", "2026-11-01", "2026-12-01");
        assertDoesNotThrow(() -> service.saveMember(valid));
        assertEquals(1, service.members().size());
        assertEquals("Juan Perez", service.members().get(0).fullName());
    }

    @Test
    @DisplayName("deleteMember debe realizar baja lógica marcando el socio como Inactivo")
    void shouldDeactivateMemberOnDelete() {
        Member valid = new Member(1, "12345678", "Juan Perez", "999888777", "juan@test.com", "Activo", "Mensual", "2026-11-01", "2026-12-01");
        repository.saveMember(valid);

        service.deleteMember(1);

        Member updated = service.findMemberById(1).orElseThrow();
        assertEquals("Inactivo", updated.status(), "El socio debe pasar a estado Inactivo (baja lógica)");
    }

    @Test
    @DisplayName("calculateMembershipStartDate debe aplicar renovación anticipada sumando 1 día si está activa")
    void shouldCalculateAdvanceRenewalStartDate() {
        LocalDate futureEnd = LocalDate.now().plusDays(10);
        repository.saveMembership(new Membership(1, 1, 1, "Mensual", LocalDate.now().minusDays(20).toString(), futureEnd.toString(), 80.0, "Activa"));

        LocalDate calculated = service.calculateMembershipStartDate(1, LocalDate.now());
        assertEquals(futureEnd.plusDays(1), calculated);
    }

    @Test
    @DisplayName("Ofertas y promociones: cálculo porcentual y monto fijo")
    void shouldCalculateDiscountsCorrectly() {
        Offer pctOffer = new Offer(1, "Promo 20%", "PERCENTAGE", 20.0, null, null, true, "20% desc");
        assertEquals(20.0, pctOffer.calculateDiscount(100.0));
        assertEquals(16.0, pctOffer.calculateDiscount(80.0));

        Offer fixedOffer = new Offer(2, "Bono S/ 15", "FIXED", 15.0, null, null, true, "S/ 15 desc");
        assertEquals(15.0, fixedOffer.calculateDiscount(100.0));
        assertEquals(10.0, fixedOffer.calculateDiscount(10.0));
    }

    @Test
    @DisplayName("assignRoutine asigna una rutina estructurada con lista de ejercicios")
    void shouldAssignStructuredRoutine() {
        Member member = new Member(1, "70010001", "Ana Torres", "999111222", "ana@test.com", "Activo", "Mensual", "2026-11-01", "2026-12-01");
        repository.saveMember(member);

        List<RoutineExercise> exercises = List.of(
                new RoutineExercise(1, 0, "Press de banca plano", 4, "10-12", 60, "RPE 8"),
                new RoutineExercise(2, 0, "Fondos en paralelas", 3, "Al fallo", 90, "Cuerpo inclinado")
        );

        service.assignRoutine(1, "Torso Hipertrofia", "Hipertrofia", "Descanso adecuado", exercises);

        List<Routine> routines = service.routinesByMember(1);
        assertEquals(1, routines.size());
        assertEquals("Torso Hipertrofia", routines.get(0).name());
        assertEquals(2, routines.get(0).exercises().size());
    }

    @Test
    @DisplayName("expiringMembers detecta socios activos con vencimiento próximo")
    void shouldDetectExpiringMembers() {
        LocalDate today = LocalDate.now();
        Member expiring = new Member(1, "70010001", "Ana Torres", "999111222", "ana@test.com", "Activo", "Mensual", today.minusDays(25).toString(), today.plusDays(5).toString());
        Member farFuture = new Member(2, "70010002", "Luis Mendoza", "999333444", "luis@test.com", "Activo", "Trimestral", today.minusDays(10).toString(), today.plusDays(40).toString());
        repository.saveMember(expiring);
        repository.saveMember(farFuture);

        List<Member> list = service.expiringMembers(7);
        assertEquals(1, list.size());
        assertEquals("Ana Torres", list.get(0).fullName());
    }

    @Test
    @DisplayName("checkIn con socio activo registra ingreso y confirma acceso concedido")
    void shouldAllowCheckInForActiveMember() {
        Member activeMember = new Member(1, "70010001", "Ana Torres", "999111222", "ana@test.com", "Activo", "Mensual", LocalDate.now().toString(), LocalDate.now().plusDays(30).toString());
        repository.saveMember(activeMember);

        CheckInResult result = service.checkIn(1);
        assertNotNull(result.attendance());
        assertFalse(result.isExpiredOrInactive());
        assertTrue(result.message().contains("concedido"));
    }

    @Test
    @DisplayName("generateReport calcula ingresos netos excluyendo cobros anulados")
    void shouldCalculateReportSummary() {
        Member member = new Member(1, "70010001", "Ana Torres", "999111222", "ana@test.com", "Activo", "Mensual", "2026-11-01", "2026-12-01");
        repository.saveMember(member);

        LocalDate today = LocalDate.now();
        service.addPayment(1, 100.0, null, "Efectivo", "Cobro 1");
        service.addPayment(1, 200.0, null, "Tarjeta", "Cobro 2");

        Payment p2 = service.payments().stream()
                .filter(p -> p.amount() == 200.0)
                .findFirst()
                .orElseThrow();
        service.cancelPayment(p2.id(), "Error de cobro");

        ReportSummary summary = service.generateReport(today.minusDays(1), today.plusDays(1));
        assertEquals(1, summary.activePaymentsCount());
        assertEquals(1, summary.cancelledPaymentsCount());
        assertEquals(100.0, summary.grossIncome());
    }

    @Test
    @DisplayName("exportReceiptPdf genera archivo PDF de recibo válido")
    void shouldExportReceiptPdf() throws IOException {
        Member member = new Member(1, "70010001", "Ana Torres", "999111222", "ana@test.com", "Activo", "Mensual", "2026-11-01", "2026-12-01");
        repository.saveMember(member);
        service.addPayment(1, 80.0, null, "Efectivo", "Mensualidad");

        Payment p = service.payments().get(0);
        File pdfFile = tempDir.resolve("test_recibo.pdf").toFile();

        service.exportReceiptPdf(p, pdfFile);
        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 500, "El archivo PDF debe contener datos");
    }

    @Test
    @DisplayName("saveConfig actualiza los datos comerciales de GymDesk")
    void shouldUpdateGymConfig() {
        GymConfig newConfig = new GymConfig("MegaGym Pro", "20112233445", "Calle 10", "999000111", "admin@megagym.com", "S/", "Gracias.");
        service.saveConfig(newConfig);

        GymConfig current = service.config();
        assertEquals("MegaGym Pro", current.gymName());
        assertEquals("20112233445", current.taxId());
    }

    @Test
    @DisplayName("authenticate valida credenciales y asigna usuario actual")
    void shouldAuthenticateValidUser() {
        User user = service.authenticate("admin", "admin123");
        assertNotNull(user);
        assertEquals("admin", user.username());
        assertTrue(service.isAuthenticated());
        assertTrue(service.isAdmin());
        assertEquals("admin", service.getCurrentUser().orElseThrow().username());

        User recep = service.authenticate("recepcion", "recepcion123");
        assertNotNull(recep);
        assertTrue(service.isAuthenticated());
        assertFalse(service.isAdmin());
        assertTrue(recep.isReceptionist());
    }

    @Test
    @DisplayName("authenticate rechaza usuario desconocido o contraseña inválida")
    void shouldRejectInvalidCredentials() {
        assertThrows(IllegalArgumentException.class, () -> service.authenticate("desconocido", "123456"));
        assertThrows(IllegalArgumentException.class, () -> service.authenticate("admin", "incorrecta"));
        assertThrows(IllegalArgumentException.class, () -> service.authenticate("", ""));
    }

    @Test
    @DisplayName("authenticate bloquea el ingreso de usuarios inactivos")
    void shouldBlockInactiveUser() {
        repository.toggleUserStatus(2, false); // Desactivar recepcion
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.authenticate("recepcion", "recepcion123"));
        assertTrue(ex.getMessage().contains("inactiva"));
    }

    @Test
    @DisplayName("Control de acceso RBAC: recepcionista no puede anular cobros ni modificar configuración")
    void shouldEnforceRbacForReceptionist() {
        service.authenticate("recepcion", "recepcion123");

        SecurityException exCancel = assertThrows(SecurityException.class, () -> service.cancelPayment(1, "Motivo test"));
        assertTrue(exCancel.getMessage().contains("ADMINISTRADOR"));

        SecurityException exConfig = assertThrows(SecurityException.class, () -> service.saveConfig(GymConfig.defaultConfig()));
        assertTrue(exConfig.getMessage().contains("ADMINISTRADOR"));

        SecurityException exUser = assertThrows(SecurityException.class, () -> service.createUser("nuevo", "123456", "Nuevo", "RECEPCIONISTA"));
        assertTrue(exUser.getMessage().contains("ADMINISTRADOR"));

        SecurityException exBackup = assertThrows(SecurityException.class, () -> service.createBackup(tempDir.toFile()));
        assertTrue(exBackup.getMessage().contains("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Administrador puede crear y gestionar usuarios")
    void shouldAllowAdminToManageUsers() {
        service.authenticate("admin", "admin123");

        service.createUser("carlos", "clave123", "Carlos Trainer", User.ROLE_RECEPTIONIST);
        assertEquals(3, service.users().size());

        User carlos = repository.findUserByUsername("carlos").orElseThrow();
        assertEquals("Carlos Trainer", carlos.fullName());
        assertTrue(carlos.isReceptionist());
        assertTrue(carlos.active());

        // Cambiar estado a inactivo
        service.toggleUserStatus(carlos.id(), false);
        assertFalse(repository.findUserById(carlos.id()).orElseThrow().active());
    }

    @Test
    @DisplayName("No se puede desactivar al único administrador activo ni a la propia cuenta activa")
    void shouldPreventDeactivatingSoleAdminOrSelf() {
        service.authenticate("admin", "admin123");
        User admin = service.getCurrentUser().orElseThrow();

        // Intentar desactivar la propia cuenta en sesión
        IllegalStateException exSelf = assertThrows(IllegalStateException.class, () -> service.toggleUserStatus(admin.id(), false));
        assertTrue(exSelf.getMessage().contains("su propia cuenta"));
    }

    @Test
    @DisplayName("changePassword permite cambiar contraseña verificando la actual")
    void shouldChangeUserPassword() {
        service.authenticate("recepcion", "recepcion123");
        User recep = service.getCurrentUser().orElseThrow();

        // Falla con contraseña actual errónea
        assertThrows(IllegalArgumentException.class, () -> service.changePassword(recep.id(), "wrongpass", "nueva123"));

        // Éxito con contraseña actual correcta
        assertDoesNotThrow(() -> service.changePassword(recep.id(), "recepcion123", "nuevaClave456"));

        // Se puede iniciar sesión con la nueva contraseña
        assertDoesNotThrow(() -> service.authenticate("recepcion", "nuevaClave456"));
    }

    @Test
    @DisplayName("createBackup genera archivo y registra la fecha de respaldo")
    void shouldCreateBackup() {
        service.authenticate("admin", "admin123");
        String backupPath = service.createBackup(tempDir.toFile());
        assertNotNull(backupPath);
        assertTrue(new File(backupPath).exists());
        assertNotEquals("Nunca", service.getLastBackupDate());
    }

    @Test
    @DisplayName("Auditoría registra automáticamente eventos críticos como cobros, anulaciones y cambios de usuario")
    void shouldAuditCriticalEventsAutomatically() {
        service.authenticate("admin", "admin123");

        Member member = new Member(0, "88776655", "Diego Lopez", "955111222", "diego@test.com", "Activo", "Mensual", "2026-11-01", "2026-12-01");
        service.saveMember(member);

        List<AuditLog> logs = service.auditLogs();
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().anyMatch(l -> AuditLog.ACTION_CREATE_MEMBER.equals(l.action())));

        // Registrar y anular cobro
        service.addPayment(1, 100.0, null, "Yape", "Mensualidad");
        service.cancelPayment(1, "Error de monto");

        List<AuditLog> updatedLogs = service.auditLogs();
        assertTrue(updatedLogs.stream().anyMatch(l -> AuditLog.ACTION_CANCEL_PAYMENT.equals(l.action())));
        assertTrue(updatedLogs.stream().anyMatch(l -> l.details().contains("Error de monto")));
    }

    // Repositorio en memoria para pruebas
    private static class InMemoryGymRepository implements GymRepository {
        private final List<Member> members = new ArrayList<>();
        private final List<Plan> plans = new ArrayList<>();
        private final List<Membership> memberships = new ArrayList<>();
        private final List<Offer> offers = new ArrayList<>();
        private final List<Payment> payments = new ArrayList<>();
        private final List<Exercise> exercises = new ArrayList<>();
        private final List<Routine> routines = new ArrayList<>();
        private final List<Attendance> attendances = new ArrayList<>();
        private final List<User> users = new ArrayList<>();
        private final List<AuditLog> auditLogs = new ArrayList<>();
        private GymConfig config = GymConfig.defaultConfig();
        private String lastBackupDate = "Nunca";

        public InMemoryGymRepository() {
            String adminSalt = PasswordUtil.generateSalt();
            users.add(new User(1, "admin", PasswordUtil.hashPassword("admin123", adminSalt), adminSalt, "Admin Test", User.ROLE_ADMIN, true, "2026-11-01"));
            String recSalt = PasswordUtil.generateSalt();
            users.add(new User(2, "recepcion", PasswordUtil.hashPassword("recepcion123", recSalt), recSalt, "Recep Test", User.ROLE_RECEPTIONIST, true, "2026-11-01"));
        }

        @Override
        public List<Member> members() {
            return new ArrayList<>(members);
        }

        @Override
        public Optional<Member> findMemberById(int id) {
            return members.stream().filter(m -> m.id() == id).findFirst();
        }

        @Override
        public void saveMember(Member m) {
            members.removeIf(existing -> existing.id() == m.id());
            int newId = (m.id() == 0) ? members.size() + 1 : m.id();
            members.add(new Member(newId, m.document(), m.fullName(), m.phone(), m.email(), m.status(), m.plan(), m.startDate(), m.endDate()));
        }

        @Override
        public void updateMemberPlanAndDates(int memberId, String planName, String startDate, String endDate, String status) {
            findMemberById(memberId).ifPresent(m -> {
                members.removeIf(existing -> existing.id() == memberId);
                members.add(new Member(m.id(), m.document(), m.fullName(), m.phone(), m.email(), status, planName, startDate, endDate));
            });
        }

        @Override
        public void deactivateMember(int id) {
            findMemberById(id).ifPresent(m -> {
                members.removeIf(existing -> existing.id() == id);
                members.add(new Member(m.id(), m.document(), m.fullName(), m.phone(), m.email(), "Inactivo", m.plan(), m.startDate(), m.endDate()));
            });
        }

        @Override
        public void deleteMember(int id) {
            deactivateMember(id);
        }

        @Override
        public List<Plan> plans() {
            return new ArrayList<>(plans);
        }

        @Override
        public Optional<Plan> findPlanById(int id) {
            return plans.stream().filter(p -> p.id() == id).findFirst();
        }

        @Override
        public void savePlan(Plan plan) {
            int newId = (plan.id() == 0) ? plans.size() + 1 : plan.id();
            plans.add(new Plan(newId, plan.name(), plan.durationDays(), plan.price(), plan.description()));
        }

        @Override
        public void updatePlan(Plan plan) {
            plans.removeIf(p -> p.id() == plan.id());
            plans.add(plan);
        }

        @Override
        public List<Membership> membershipsByMember(int memberId) {
            return memberships.stream().filter(ms -> ms.memberId() == memberId).toList();
        }

        @Override
        public Optional<Membership> findLatestMembership(int memberId) {
            return memberships.stream()
                    .filter(ms -> ms.memberId() == memberId)
                    .max(Comparator.comparing(Membership::endDate));
        }

        @Override
        public void saveMembership(Membership membership) {
            int newId = (membership.id() == 0) ? memberships.size() + 1 : membership.id();
            memberships.add(new Membership(
                    newId, membership.memberId(), membership.planId(), membership.planName(),
                    membership.startDate(), membership.endDate(), membership.pricePaid(), membership.status()
            ));
        }

        @Override
        public List<Offer> offers() {
            return new ArrayList<>(offers);
        }

        @Override
        public Optional<Offer> findOfferById(int id) {
            return offers.stream().filter(o -> o.id() == id).findFirst();
        }

        @Override
        public void saveOffer(Offer offer) {
            int newId = (offer.id() == 0) ? offers.size() + 1 : offer.id();
            offers.add(new Offer(newId, offer.name(), offer.discountType(), offer.discountValue(), offer.validUntil(), offer.planId(), offer.active(), offer.description()));
        }

        @Override
        public void toggleOfferStatus(int id, boolean active) {
            findOfferById(id).ifPresent(o -> {
                offers.removeIf(existing -> existing.id() == id);
                offers.add(new Offer(o.id(), o.name(), o.discountType(), o.discountValue(), o.validUntil(), o.planId(), active, o.description()));
            });
        }

        @Override
        public List<Payment> payments() {
            return new ArrayList<>(payments);
        }

        @Override
        public List<Payment> paymentsBetween(String startDate, String endDate) {
            return new ArrayList<>(payments);
        }

        @Override
        public Optional<Payment> findPaymentById(int id) {
            return payments.stream().filter(p -> p.id() == id).findFirst();
        }

        @Override
        public void addPayment(int memberId, double originalAmount, double discountApplied, double finalAmount, String promotionName, String method, String concept) {
            int newId = payments.size() + 1;
            String memberName = findMemberById(memberId).map(Member::fullName).orElse("Socio " + memberId);
            payments.add(new Payment(
                    newId, memberId, memberName, originalAmount, discountApplied, finalAmount,
                    promotionName, method, concept, LocalDate.now().toString(), "BOL-" + (1000 + newId),
                    "Registrado", ""
            ));
        }

        @Override
        public void cancelPayment(int paymentId, String reason) {
            findPaymentById(paymentId).ifPresent(p -> {
                payments.removeIf(existing -> existing.id() == paymentId);
                payments.add(new Payment(
                        p.id(), p.memberId(), p.memberName(), p.originalAmount(), p.discountApplied(),
                        p.amount(), p.promotionName(), p.method(), p.concept(), p.date(), p.invoiceNumber(),
                        "Anulado", reason
                ));
            });
        }

        @Override
        public List<Exercise> exercises() {
            return new ArrayList<>(exercises);
        }

        @Override
        public void saveExercise(Exercise exercise) {
            exercises.add(exercise);
        }

        @Override
        public List<Routine> routines() {
            return new ArrayList<>(routines);
        }

        @Override
        public List<Routine> routinesByMember(int memberId) {
            return routines.stream().filter(r -> r.memberId() == memberId).toList();
        }

        @Override
        public void saveRoutine(Routine routine) {
            int newId = (routine.id() == 0) ? routines.size() + 1 : routine.id();
            routines.add(new Routine(
                    newId, routine.memberId(), routine.memberName(), routine.name(),
                    routine.goal(), routine.notes(), routine.createdAt(), routine.exercises()
            ));
        }

        @Override
        public List<Attendance> attendance() {
            return new ArrayList<>(attendances);
        }

        @Override
        public List<Attendance> attendanceByMember(int memberId) {
            return attendances.stream().filter(a -> a.memberId() == memberId).toList();
        }

        @Override
        public int countAttendanceBetween(String startDate, String endDate) {
            return attendances.size();
        }

        @Override
        public Attendance checkIn(int memberId) {
            Member m = findMemberById(memberId).orElseThrow();
            int newId = attendances.size() + 1;
            Attendance a = new Attendance(newId, memberId, m.fullName(), m.document(), LocalDate.now().toString() + " 10:00:00", m.status(), m.plan());
            attendances.add(a);
            return a;
        }

        @Override
        public GymConfig config() {
            return config;
        }

        @Override
        public void saveConfig(GymConfig config) {
            this.config = config;
        }

        @Override
        public int count(String table) {
            return switch (table) {
                case "members" -> members.size();
                case "plans" -> plans.size();
                case "memberships" -> memberships.size();
                case "offers" -> offers.size();
                case "payments" -> payments.size();
                case "routines" -> routines.size();
                case "attendance" -> attendances.size();
                case "users" -> users.size();
                case "audit_logs" -> auditLogs.size();
                default -> 0;
            };
        }

        @Override
        public double incomeThisMonth() {
            return payments.stream()
                    .filter(p -> !"Anulado".equalsIgnoreCase(p.status()))
                    .mapToDouble(Payment::amount)
                    .sum();
        }

        @Override
        public void addAuditLog(AuditLog log) {
            int newId = auditLogs.size() + 1;
            auditLogs.add(new AuditLog(newId, log.timestamp(), log.username(), log.action(), log.details()));
        }

        @Override
        public List<AuditLog> auditLogs() {
            return new ArrayList<>(auditLogs);
        }

        @Override
        public List<AuditLog> recentAuditLogs(int limit) {
            return auditLogs.stream()
                    .sorted(Comparator.comparingInt(AuditLog::id).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<User> users() {
            return new ArrayList<>(users);
        }

        @Override
        public Optional<User> findUserById(int id) {
            return users.stream().filter(u -> u.id() == id).findFirst();
        }

        @Override
        public Optional<User> findUserByUsername(String username) {
            return users.stream().filter(u -> u.username().equalsIgnoreCase(username)).findFirst();
        }

        @Override
        public void saveUser(User user) {
            int newId = (user.id() == 0) ? users.size() + 1 : user.id();
            users.add(new User(newId, user.username(), user.passwordHash(), user.salt(), user.fullName(), user.role(), user.active(), user.createdAt()));
        }

        @Override
        public void updateUser(User user) {
            users.removeIf(u -> u.id() == user.id());
            users.add(user);
        }

        @Override
        public void updateUserPassword(int userId, String newHash, String newSalt) {
            findUserById(userId).ifPresent(u -> {
                users.removeIf(existing -> existing.id() == userId);
                users.add(new User(u.id(), u.username(), newHash, newSalt, u.fullName(), u.role(), u.active(), u.createdAt()));
            });
        }

        @Override
        public void toggleUserStatus(int id, boolean active) {
            findUserById(id).ifPresent(u -> {
                users.removeIf(existing -> existing.id() == id);
                users.add(new User(u.id(), u.username(), u.passwordHash(), u.salt(), u.fullName(), u.role(), active, u.createdAt()));
            });
        }

        @Override
        public String createBackup(File targetDirectory) {
            this.lastBackupDate = LocalDate.now().toString();
            File backup = new File(targetDirectory, "gymdesk_backup_test.db");
            try {
                backup.createNewFile();
            } catch (IOException ignored) {
            }
            return backup.getAbsolutePath();
        }

        @Override
        public void restoreBackup(File backupFile) {
            if (backupFile == null || !backupFile.exists()) {
                throw new IllegalArgumentException("Archivo inexistente");
            }
        }

        @Override
        public String getLastBackupDate() {
            return lastBackupDate;
        }
    }
}
