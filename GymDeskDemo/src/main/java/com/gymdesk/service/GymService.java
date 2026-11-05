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

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio principal con la lógica de negocio y reglas de aplicación de GymDesk.
 */
public class GymService {

    private final GymRepository repo;
    private final ReceiptService receiptService;
    private User currentUser;

    public GymService(GymRepository repo) {
        this.repo = Objects.requireNonNull(repo, "El repositorio no puede ser nulo.");
        this.receiptService = new ReceiptService();
    }

    public GymService(GymRepository repo, ReceiptService receiptService) {
        this.repo = Objects.requireNonNull(repo, "El repositorio no puede ser nulo.");
        this.receiptService = Objects.requireNonNull(receiptService, "El servicio de recibos no puede ser nulo.");
    }

    // ==========================================
    // AUTENTICACIÓN, SESIÓN Y CONTROL DE ACCESO (RBAC)
    // ==========================================

    public User authenticate(String username, String password) {
        if (username == null || username.trim().isBlank()) {
            throw new IllegalArgumentException("Debe ingresar su nombre de usuario.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar su contraseña.");
        }

        Optional<User> userOpt = repo.findUserByUsername(username.trim());
        if (userOpt.isEmpty()) {
            logAudit(AuditLog.ACTION_LOGIN, "Intento fallido de inicio de sesión: usuario '" + username.trim() + "' no encontrado.");
            throw new IllegalArgumentException("Credenciales inválidas: usuario o contraseña incorrectos.");
        }

        User user = userOpt.get();

        if (!user.active()) {
            logAudit(AuditLog.ACTION_LOGIN, "Intento fallido de inicio de sesión: cuenta inactiva para '" + user.username() + "'.");
            throw new IllegalStateException("La cuenta de usuario '" + user.username() + "' está inactiva. Contacte al administrador.");
        }

        if (!PasswordUtil.verifyPassword(password, user.salt(), user.passwordHash())) {
            logAudit(AuditLog.ACTION_LOGIN, "Intento fallido de inicio de sesión: contraseña errónea para '" + user.username() + "'.");
            throw new IllegalArgumentException("Credenciales inválidas: usuario o contraseña incorrectos.");
        }

        this.currentUser = user;
        logAudit(AuditLog.ACTION_LOGIN, "Inicio de sesión exitoso. Rol: " + user.role());
        return user;
    }

    public void logout() {
        if (currentUser != null) {
            logAudit(AuditLog.ACTION_LOGOUT, "Cierre de sesión voluntario.");
        }
        this.currentUser = null;
    }

    public void logAudit(String action, String details) {
        String username = (currentUser != null) ? currentUser.username() : "SISTEMA";
        String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        repo.addAuditLog(new AuditLog(0, now, username, action, details));
    }

    public List<AuditLog> auditLogs() {
        requireAdmin("consultar los registros de auditoría");
        return repo.auditLogs();
    }

    public List<AuditLog> recentAuditLogs(int limit) {
        requireAdmin("consultar los registros de auditoría");
        return repo.recentAuditLogs(limit);
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public void requireAdmin(String actionDescription) {
        if (currentUser != null && !currentUser.isAdmin()) {
            throw new SecurityException("Acceso denegado: Se requieren permisos de ADMINISTRADOR para " + actionDescription + ".");
        }
    }

    // ==========================================
    // SOCIOS
    // ==========================================

    public List<Member> members() {
        return repo.members();
    }

    public Optional<Member> findMemberById(int id) {
        if (id <= 0) return Optional.empty();
        return repo.findMemberById(id);
    }

    public Optional<Member> findMemberByDocument(String document) {
        if (document == null || document.trim().isBlank()) return Optional.empty();
        String doc = document.trim();
        return repo.members().stream()
                .filter(m -> doc.equalsIgnoreCase(m.document()))
                .findFirst();
    }

    public List<Member> searchMembers(String query) {
        if (query == null || query.trim().isBlank()) {
            return members();
        }
        String q = query.trim().toLowerCase();
        return repo.members().stream()
                .filter(m -> m.document().toLowerCase().contains(q) || m.fullName().toLowerCase().contains(q))
                .toList();
    }

    public void saveMember(Member m) {
        if (m == null) {
            throw new IllegalArgumentException("Los datos del socio no pueden ser nulos.");
        }
        if (m.document() == null || m.document().trim().isBlank()) {
            throw new IllegalArgumentException("El número de documento es obligatorio.");
        }
        if (m.fullName() == null || m.fullName().trim().isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio.");
        }
        repo.saveMember(m);
        logAudit(m.id() == 0 ? AuditLog.ACTION_CREATE_MEMBER : AuditLog.ACTION_UPDATE_MEMBER,
                "Socio: " + m.fullName().trim() + " (" + m.document().trim() + ") · Plan: " + m.plan());
    }

    public void deactivateMember(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de socio inválido.");
        }
        repo.deactivateMember(id);
        logAudit(AuditLog.ACTION_DEACTIVATE_MEMBER, "Baja lógica de socio con ID " + id);
    }

    public void deleteMember(int id) {
        deactivateMember(id);
    }

    public List<Member> expiringMembers(int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(withinDays);
        return repo.members().stream()
                .filter(Member::isActive)
                .filter(m -> {
                    try {
                        LocalDate end = LocalDate.parse(m.endDate());
                        return !end.isBefore(today) && !end.isAfter(limit);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
    }

    public List<Member> expiredMembers() {
        LocalDate today = LocalDate.now();
        return repo.members().stream()
                .filter(m -> {
                    if ("Vencido".equalsIgnoreCase(m.status())) return true;
                    try {
                        return LocalDate.parse(m.endDate()).isBefore(today);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
    }

    // ==========================================
    // PLANES
    // ==========================================

    public List<Plan> plans() {
        return repo.plans();
    }

    public Optional<Plan> findPlanById(int id) {
        if (id <= 0) return Optional.empty();
        return repo.findPlanById(id);
    }

    public void savePlan(String name, int durationDays, double price, String description) {
        requireAdmin("crear planes de membresía");
        validatePlanInput(name, durationDays, price);
        repo.savePlan(new Plan(0, name.trim(), durationDays, price, description != null ? description.trim() : ""));
    }

    public void updatePlan(int id, String name, int durationDays, double price, String description) {
        requireAdmin("modificar planes de membresía");
        if (id <= 0) {
            throw new IllegalArgumentException("ID de plan inválido.");
        }
        validatePlanInput(name, durationDays, price);
        repo.updatePlan(new Plan(id, name.trim(), durationDays, price, description != null ? description.trim() : ""));
    }

    private void validatePlanInput(String name, int durationDays, double price) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre del plan es obligatorio.");
        }
        if (durationDays < 1) {
            throw new IllegalArgumentException("La duración del plan debe ser de al menos 1 día.");
        }
        if (price < 0) {
            throw new IllegalArgumentException("El precio del plan no puede ser negativo.");
        }
    }

    // ==========================================
    // MEMBRESÍAS Y RENOVACIONES
    // ==========================================

    public LocalDate calculateMembershipStartDate(int memberId, LocalDate baseDate) {
        LocalDate start = (baseDate != null) ? baseDate : LocalDate.now();
        Optional<Membership> latestOpt = repo.findLatestMembership(memberId);
        if (latestOpt.isPresent()) {
            Membership latest = latestOpt.get();
            try {
                LocalDate currentEnd = LocalDate.parse(latest.endDate());
                LocalDate today = LocalDate.now();
                if (!currentEnd.isBefore(today)) {
                    return currentEnd.plusDays(1);
                }
            } catch (Exception ignored) {
            }
        }
        return start;
    }

    public Membership assignOrRenewMembership(int memberId, int planId, LocalDate requestedStartDate) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un socio válido.");
        }
        Plan plan = repo.findPlanById(planId)
                .orElseThrow(() -> new IllegalArgumentException("El plan seleccionado (ID " + planId + ") no existe."));

        LocalDate startDate = calculateMembershipStartDate(memberId, requestedStartDate);
        LocalDate endDate = startDate.plusDays(plan.durationDays());

        String status = endDate.isBefore(LocalDate.now()) ? "Vencida" : "Activa";
        String memberStatus = "Activa".equalsIgnoreCase(status) ? "Activo" : "Vencido";

        Membership membership = new Membership(
                0,
                memberId,
                plan.id(),
                plan.name(),
                startDate.toString(),
                endDate.toString(),
                plan.price(),
                status
        );

        repo.saveMembership(membership);
        repo.updateMemberPlanAndDates(memberId, plan.name(), startDate.toString(), endDate.toString(), memberStatus);
        logAudit(AuditLog.ACTION_RENEW_MEMBERSHIP, "Socio ID " + memberId + " activó membresía '" + plan.name() +
                "' vigencia: " + startDate + " a " + endDate + " por " + String.format(Locale.US, "%.2f", plan.price()));

        return membership;
    }

    public List<Membership> membershipsByMember(int memberId) {
        if (memberId <= 0) return List.of();
        return repo.membershipsByMember(memberId);
    }

    public Optional<Membership> latestMembership(int memberId) {
        if (memberId <= 0) return Optional.empty();
        return repo.findLatestMembership(memberId);
    }

    // ==========================================
    // OFERTAS Y PROMOCIONES (MODELO MIXTO)
    // ==========================================

    public List<Offer> offers() {
        return repo.offers();
    }

    public Optional<Offer> findOfferById(int id) {
        if (id <= 0) return Optional.empty();
        return repo.findOfferById(id);
    }

    public List<Offer> applicableOffers(Integer planId) {
        return repo.offers().stream()
                .filter(Offer::isValid)
                .filter(o -> o.planId() == null || Objects.equals(o.planId(), planId))
                .toList();
    }

    public void saveOffer(String name, String discountType, double discountValue, String validUntil, Integer planId, String description) {
        requireAdmin("crear promociones u ofertas");
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la promoción es obligatorio.");
        }
        if (!"PERCENTAGE".equalsIgnoreCase(discountType) && !"FIXED".equalsIgnoreCase(discountType)) {
            throw new IllegalArgumentException("El tipo de descuento debe ser PERCENTAGE o FIXED.");
        }
        if (discountValue <= 0) {
            throw new IllegalArgumentException("El valor del descuento debe ser mayor a cero.");
        }
        if ("PERCENTAGE".equalsIgnoreCase(discountType) && discountValue > 100) {
            throw new IllegalArgumentException("Un descuento porcentual no puede exceder el 100%.");
        }
        if (validUntil != null && !validUntil.trim().isBlank()) {
            try {
                LocalDate.parse(validUntil.trim());
            } catch (Exception e) {
                throw new IllegalArgumentException("Formato de fecha de vigencia inválido (se espera YYYY-MM-DD).");
            }
        }
        repo.saveOffer(new Offer(
                0,
                name.trim(),
                discountType.toUpperCase(),
                discountValue,
                (validUntil != null && !validUntil.trim().isBlank()) ? validUntil.trim() : null,
                planId,
                true,
                description != null ? description.trim() : ""
        ));
    }

    public void toggleOfferStatus(int id, boolean active) {
        requireAdmin("activar o desactivar promociones");
        if (id <= 0) throw new IllegalArgumentException("ID de oferta inválido.");
        repo.toggleOfferStatus(id, active);
    }

    // ==========================================
    // PAGOS / COBROS Y CAJA
    // ==========================================

    public List<Payment> payments() {
        return repo.payments();
    }

    public Optional<Payment> findPaymentById(int id) {
        if (id <= 0) return Optional.empty();
        return repo.findPaymentById(id);
    }

    public void addPayment(int memberId, double originalAmount, Integer offerId, String method, String concept) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un socio válido.");
        }
        if (originalAmount <= 0) {
            throw new IllegalArgumentException("El importe original debe ser mayor a cero.");
        }
        if (method == null || method.trim().isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar un método de pago.");
        }
        if (concept == null || concept.trim().isBlank()) {
            throw new IllegalArgumentException("El concepto de cobro es obligatorio.");
        }

        double discount = 0.0;
        String promotionName = "Ninguna";

        if (offerId != null && offerId > 0) {
            Optional<Offer> offerOpt = repo.findOfferById(offerId);
            if (offerOpt.isPresent() && offerOpt.get().isValid()) {
                Offer offer = offerOpt.get();
                discount = offer.calculateDiscount(originalAmount);
                promotionName = offer.name();
            }
        }

        double finalAmount = Math.max(0.0, originalAmount - discount);
        repo.addPayment(memberId, originalAmount, discount, finalAmount, promotionName, method.trim(), concept.trim());
        logAudit(AuditLog.ACTION_PAYMENT, "Cobro de S/ " + String.format(Locale.US, "%.2f", finalAmount) +
                " para socio ID " + memberId + " (" + concept.trim() + ") por " + method.trim());
    }

    public void addPayment(int memberId, double amount, String method, String concept) {
        addPayment(memberId, amount, null, method, concept);
    }

    public void cancelPayment(int paymentId, String reason) {
        requireAdmin("anular cobros");
        if (paymentId <= 0) {
            throw new IllegalArgumentException("ID de cobro inválido.");
        }
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("Debe especificar un motivo justificado para la anulación.");
        }

        Payment payment = repo.findPaymentById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("El cobro indicado no existe."));

        if (payment.isCancelled()) {
            throw new IllegalStateException("El cobro ya se encuentra anulado.");
        }

        repo.cancelPayment(paymentId, reason.trim());
        logAudit(AuditLog.ACTION_CANCEL_PAYMENT, "Comprobante " + payment.invoiceNumber() + " (ID " + paymentId +
                ", importe " + payment.amount() + ") anulado. Motivo: " + reason.trim());
    }

    // ==========================================
    // CATÁLOGO DE EJERCICIOS Y RUTINAS
    // ==========================================

    public List<Exercise> exercises() {
        return repo.exercises();
    }

    public void saveExercise(String name, String muscleGroup, String description) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre del ejercicio es obligatorio.");
        }
        if (muscleGroup == null || muscleGroup.trim().isBlank()) {
            throw new IllegalArgumentException("El grupo muscular es obligatorio.");
        }
        repo.saveExercise(new Exercise(0, name.trim(), muscleGroup.trim(), description != null ? description.trim() : ""));
    }

    public List<Routine> routines() {
        return repo.routines();
    }

    public List<Routine> routinesByMember(int memberId) {
        if (memberId <= 0) return List.of();
        return repo.routinesByMember(memberId);
    }

    public void assignRoutine(int memberId, String routineName, String goal, String notes, List<RoutineExercise> exercises) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un socio válido.");
        }
        if (routineName == null || routineName.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la rutina es obligatorio.");
        }

        Member member = repo.findMemberById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("El socio indicado no existe."));

        Routine routine = new Routine(
                0,
                memberId,
                member.fullName(),
                routineName.trim(),
                goal != null && !goal.trim().isBlank() ? goal.trim() : "Acondicionamiento general",
                notes != null ? notes.trim() : "",
                LocalDate.now().toString(),
                exercises != null ? exercises : List.of()
        );

        repo.saveRoutine(routine);
    }

    public void addRoutine(int memberId, String title, String details) {
        assignRoutine(memberId, title, "Acondicionamiento", details, List.of());
    }

    // ==========================================
    // ASISTENCIA
    // ==========================================

    public List<Attendance> attendance() {
        return repo.attendance();
    }

    public List<Attendance> attendanceByMember(int memberId) {
        if (memberId <= 0) return List.of();
        return repo.attendanceByMember(memberId);
    }

    public int countAttendanceBetween(String startDate, String endDate) {
        if (startDate == null || endDate == null) return 0;
        return repo.countAttendanceBetween(startDate, endDate);
    }

    public CheckInResult checkIn(int memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un socio válido.");
        }

        Member member = repo.findMemberById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("El socio no existe en el sistema."));

        Attendance record = repo.checkIn(memberId);

        boolean expiredOrInactive = false;
        String message;

        if ("Inactivo".equalsIgnoreCase(member.status()) || "Suspendido".equalsIgnoreCase(member.status())) {
            expiredOrInactive = true;
            message = "¡ADVERTENCIA! El socio " + member.fullName() + " se encuentra " + member.status().toUpperCase() +
                    ". Se registró el ingreso, favor de regularizar en recepción.";
        } else {
            try {
                LocalDate endDate = LocalDate.parse(member.endDate());
                if (endDate.isBefore(LocalDate.now())) {
                    expiredOrInactive = true;
                    message = "¡MEMBRESÍA VENCIDA! El socio " + member.fullName() + " tiene el plan " + member.plan() +
                            " vencido desde el " + member.endDate() + ".\nSe registró el ingreso, por favor solicitar renovación inmediata.";
                } else {
                    message = "Ingreso concedido. Socio: " + member.fullName() + " · Plan: " + member.plan() +
                            " (Vigente hasta " + member.endDate() + ").";
                }
            } catch (Exception e) {
                message = "Ingreso registrado para " + member.fullName() + ".";
            }
        }

        return new CheckInResult(record, expiredOrInactive, message);
    }

    // ==========================================
    // CONFIGURACIÓN COMERCIAL
    // ==========================================

    public GymConfig config() {
        return repo.config();
    }

    public void saveConfig(GymConfig config) {
        requireAdmin("modificar la configuración comercial");
        if (config == null) throw new IllegalArgumentException("La configuración no puede ser nula.");
        if (config.gymName() == null || config.gymName().trim().isBlank()) {
            throw new IllegalArgumentException("El nombre del gimnasio es obligatorio.");
        }
        repo.saveConfig(config);
        logAudit(AuditLog.ACTION_CONFIG_UPDATE, "Configuración actualizada. Gimnasio: '" + config.gymName() +
                "' · RUC: " + config.taxId() + " · Moneda: " + config.currency());
    }

    // ==========================================
    // REPORTES Y COMPROBANTES PDF
    // ==========================================

    public ReportSummary generateReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }

        List<Payment> list = repo.paymentsBetween(startDate.toString(), endDate.toString());
        int attendanceCount = repo.countAttendanceBetween(startDate.toString(), endDate.toString());

        double grossIncome = list.stream()
                .filter(p -> !p.isCancelled())
                .mapToDouble(Payment::amount)
                .sum();

        double totalDiscounts = list.stream()
                .filter(p -> !p.isCancelled())
                .mapToDouble(Payment::discountApplied)
                .sum();

        int activeCount = (int) list.stream().filter(p -> !p.isCancelled()).count();
        int cancelledCount = (int) list.stream().filter(Payment::isCancelled).count();

        return new ReportSummary(
                startDate.toString(),
                endDate.toString(),
                grossIncome,
                totalDiscounts,
                list.size(),
                activeCount,
                cancelledCount,
                attendanceCount,
                list
        );
    }

    public void exportReceiptPdf(Payment payment, File targetFile) throws IOException {
        receiptService.exportReceiptPdf(payment, config(), targetFile);
    }

    public void exportReportPdf(ReportSummary summary, File targetFile) throws IOException {
        receiptService.exportReportPdf(summary, config(), targetFile);
    }

    // ==========================================
    // MÉTRICAS E INDICADORES
    // ==========================================

    public int count(String table) {
        return repo.count(table);
    }

    public double income() {
        return repo.incomeThisMonth();
    }

    public String nextEndDate(int days) {
        return LocalDate.now().plusDays(days).toString();
    }

    // ==========================================
    // ADMINISTRACIÓN DE USUARIOS
    // ==========================================

    public List<User> users() {
        requireAdmin("consultar la lista de usuarios");
        return repo.users();
    }

    public void createUser(String username, String password, String fullName, String role) {
        requireAdmin("crear nuevos usuarios");
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("El nombre de usuario debe tener al menos 3 caracteres.");
        }
        String cleanUser = username.trim().toLowerCase();
        if (!cleanUser.matches("^[a-z0-9_.-]+$")) {
            throw new IllegalArgumentException("El nombre de usuario solo puede contener letras, números, puntos o guiones.");
        }
        if (repo.findUserByUsername(cleanUser).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el nombre '" + cleanUser + "'.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener un mínimo de 6 caracteres.");
        }
        if (fullName == null || fullName.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre completo del usuario es obligatorio.");
        }
        if (!User.ROLE_ADMIN.equalsIgnoreCase(role) && !User.ROLE_RECEPTIONIST.equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("El rol debe ser ADMINISTRADOR o RECEPCIONISTA.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);
        String formattedRole = User.ROLE_ADMIN.equalsIgnoreCase(role) ? User.ROLE_ADMIN : User.ROLE_RECEPTIONIST;

        User newUser = new User(0, cleanUser, hash, salt, fullName.trim(), formattedRole, true, LocalDate.now().toString());
        repo.saveUser(newUser);
        logAudit(AuditLog.ACTION_USER_CREATE, "Usuario creado: '" + cleanUser + "' (" + fullName.trim() + ") · Rol: " + formattedRole);
    }

    public void updateUser(int userId, String fullName, String role) {
        requireAdmin("editar datos de usuarios");
        if (userId <= 0) {
            throw new IllegalArgumentException("ID de usuario inválido.");
        }
        User existing = repo.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario con ID " + userId + " no existe."));

        if (fullName == null || fullName.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio.");
        }
        if (!User.ROLE_ADMIN.equalsIgnoreCase(role) && !User.ROLE_RECEPTIONIST.equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("El rol debe ser ADMINISTRADOR o RECEPCIONISTA.");
        }

        String formattedRole = User.ROLE_ADMIN.equalsIgnoreCase(role) ? User.ROLE_ADMIN : User.ROLE_RECEPTIONIST;

        if (existing.isAdmin() && !User.ROLE_ADMIN.equals(formattedRole)) {
            long activeAdmins = repo.users().stream().filter(u -> u.isAdmin() && u.active()).count();
            if (activeAdmins <= 1) {
                throw new IllegalStateException("No se puede cambiar el rol del único administrador activo del sistema.");
            }
        }

        repo.updateUser(new User(
                userId, existing.username(), existing.passwordHash(), existing.salt(),
                fullName.trim(), formattedRole, existing.active(), existing.createdAt()
        ));
        logAudit(AuditLog.ACTION_USER_TOGGLE, "Usuario '" + existing.username() + "' actualizado. Nombre: " +
                fullName.trim() + ", Rol: " + formattedRole);
    }

    public void toggleUserStatus(int userId, boolean active) {
        requireAdmin("activar o desactivar usuarios");
        if (userId <= 0) {
            throw new IllegalArgumentException("ID de usuario inválido.");
        }
        User existing = repo.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario con ID " + userId + " no existe."));

        if (currentUser != null && currentUser.id() == userId && !active) {
            throw new IllegalStateException("No puede desactivar su propia cuenta mientras tiene la sesión activa.");
        }

        if (!active && existing.isAdmin()) {
            long activeAdmins = repo.users().stream().filter(u -> u.isAdmin() && u.active()).count();
            if (activeAdmins <= 1) {
                throw new IllegalStateException("No se puede desactivar al único administrador activo del sistema.");
            }
        }

        repo.toggleUserStatus(userId, active);
        logAudit(AuditLog.ACTION_USER_TOGGLE, "Usuario '" + existing.username() + "' (ID " + userId + ") marcado como " + (active ? "ACTIVO" : "INACTIVO"));
    }

    public void changePassword(int userId, String currentPassword, String newPassword) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID de usuario inválido.");
        }
        User target = repo.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));

        if (currentUser != null && currentUser.id() == userId) {
            if (currentPassword == null || !PasswordUtil.verifyPassword(currentPassword, target.salt(), target.passwordHash())) {
                throw new IllegalArgumentException("La contraseña actual es incorrecta.");
            }
        } else {
            requireAdmin("restablecer la contraseña de otro usuario");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(newPassword, newSalt);
        repo.updateUserPassword(userId, newHash, newSalt);
        logAudit(AuditLog.ACTION_PASSWORD_CHANGE, "Contraseña actualizada para usuario '" + target.username() + "' (ID " + userId + ")");
    }

    // ==========================================
    // COPIAS DE SEGURIDAD (RESPALDOS)
    // ==========================================

    public String createBackup(File targetDirectory) {
        requireAdmin("generar copias de seguridad");
        if (targetDirectory == null) {
            throw new IllegalArgumentException("Debe seleccionar un directorio de destino para el respaldo.");
        }
        String backupPath = repo.createBackup(targetDirectory);
        logAudit(AuditLog.ACTION_BACKUP_CREATE, "Respaldo generado exitosamente: " + backupPath);
        return backupPath;
    }

    public void restoreBackup(File backupFile) {
        requireAdmin("restaurar copias de seguridad");
        if (backupFile == null) {
            throw new IllegalArgumentException("Debe seleccionar un archivo de base de datos válido.");
        }
        repo.restoreBackup(backupFile);
        logAudit(AuditLog.ACTION_BACKUP_RESTORE, "Base de datos restaurada desde: " + backupFile.getName());
    }

    public String getLastBackupDate() {
        return repo.getLastBackupDate();
    }
}
