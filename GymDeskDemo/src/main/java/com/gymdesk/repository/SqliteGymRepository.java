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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implementación de persistencia sobre SQLite mediante JDBC.
 */
public class SqliteGymRepository implements GymRepository {

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:gymdesk.db";
    private static final Set<String> ALLOWED_COUNT_TABLES = Set.of(
            "members", "plans", "payments", "routines", "attendance", "memberships", "offers", "exercises", "config", "users", "audit_logs"
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String url;

    public SqliteGymRepository() {
        this(DEFAULT_DB_URL);
    }

    public SqliteGymRepository(String url) {
        this.url = Objects.requireNonNull(url, "URL de conexión no puede ser nula");
        initDatabase();
    }

    private Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private void initDatabase() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS plans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE,
                    duration_days INTEGER,
                    price REAL,
                    description TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    document TEXT UNIQUE,
                    full_name TEXT,
                    phone TEXT,
                    email TEXT,
                    status TEXT,
                    plan TEXT,
                    start_date TEXT,
                    end_date TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS memberships (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    member_id INTEGER NOT NULL,
                    plan_id INTEGER NOT NULL,
                    start_date TEXT NOT NULL,
                    end_date TEXT NOT NULL,
                    price_paid REAL NOT NULL,
                    status TEXT NOT NULL,
                    FOREIGN KEY(member_id) REFERENCES members(id),
                    FOREIGN KEY(plan_id) REFERENCES plans(id)
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS offers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE,
                    discount_type TEXT NOT NULL,
                    discount_value REAL NOT NULL,
                    valid_until TEXT,
                    plan_id INTEGER,
                    active INTEGER NOT NULL DEFAULT 1,
                    description TEXT,
                    FOREIGN KEY(plan_id) REFERENCES plans(id)
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    member_id INTEGER,
                    original_amount REAL DEFAULT 0.0,
                    discount_applied REAL DEFAULT 0.0,
                    amount REAL,
                    promotion_name TEXT DEFAULT '',
                    method TEXT,
                    concept TEXT,
                    date TEXT,
                    invoice_number TEXT,
                    status TEXT DEFAULT 'Registrado',
                    cancellation_reason TEXT DEFAULT '',
                    FOREIGN KEY(member_id) REFERENCES members(id)
                );
            """);

            migratePaymentsColumns(stmt);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS exercises (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE,
                    muscle_group TEXT NOT NULL,
                    description TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS routines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    member_id INTEGER,
                    name TEXT,
                    goal TEXT,
                    notes TEXT,
                    created_at TEXT,
                    title TEXT,
                    details TEXT,
                    FOREIGN KEY(member_id) REFERENCES members(id)
                );
            """);

            migrateRoutinesColumns(stmt);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS routine_exercises (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    routine_id INTEGER NOT NULL,
                    exercise_name TEXT NOT NULL,
                    series INTEGER NOT NULL,
                    repetitions TEXT NOT NULL,
                    rest_seconds INTEGER NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(routine_id) REFERENCES routines(id) ON DELETE CASCADE
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    member_id INTEGER,
                    check_date TEXT,
                    FOREIGN KEY(member_id) REFERENCES members(id)
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS config (
                    key TEXT PRIMARY KEY,
                    value TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    username TEXT NOT NULL,
                    action TEXT NOT NULL,
                    details TEXT NOT NULL
                );
            """);

            // Semillas de planes
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM plans")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("""
                        INSERT INTO plans (name, duration_days, price, description) VALUES
                        ('Mensual', 30, 80.0, 'Acceso por 30 días'),
                        ('Trimestral', 90, 210.0, 'Acceso por 90 días'),
                        ('Anual', 365, 720.0, 'Acceso por 1 año');
                    """);
                }
            }

            // Semillas de socios
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM members")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("""
                        INSERT INTO members (document, full_name, phone, email, status, plan, start_date, end_date) VALUES
                        ('70010001', 'Ana Torres', '952111222', 'ana@email.com', 'Activo', 'Mensual', date('now'), date('now', '+30 day')),
                        ('70010002', 'Luis Mendoza', '952333444', 'luis@email.com', 'Activo', 'Trimestral', date('now', '-10 day'), date('now', '+80 day')),
                        ('70010003', 'Carla Rojas', '952555666', 'carla@email.com', 'Vencido', 'Mensual', date('now', '-60 day'), date('now', '-30 day'));
                    """);
                }
            }

            // Semillas de membresías
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM memberships")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("""
                        INSERT INTO memberships (member_id, plan_id, start_date, end_date, price_paid, status)
                        SELECT m.id,
                               COALESCE(p.id, (SELECT id FROM plans LIMIT 1)),
                               m.start_date,
                               m.end_date,
                               COALESCE(p.price, 80.0),
                               CASE WHEN m.status = 'Activo' THEN 'Activa' ELSE 'Vencida' END
                        FROM members m
                        LEFT JOIN plans p ON p.name = m.plan;
                    """);
                }
            }

            // Semillas de ofertas
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM offers")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("""
                        INSERT INTO offers (name, discount_type, discount_value, valid_until, plan_id, active, description) VALUES
                        ('Descuento Apertura (15%)', 'PERCENTAGE', 15.0, date('now', '+60 day'), NULL, 1, 'Descuento del 15% por campaña de apertura en cualquier plan'),
                        ('Pago en Efectivo (S/ 10)', 'FIXED', 10.0, NULL, NULL, 1, 'Descuento fijo de S/ 10 al pagar en efectivo'),
                        ('Promo Estudiante Mensual (10%)', 'PERCENTAGE', 10.0, date('now', '+180 day'), (SELECT id FROM plans WHERE name='Mensual' LIMIT 1), 1, 'Tarifa preferencial para estudiantes en plan mensual');
                    """);
                }
            }

            // Semillas del catálogo de ejercicios
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM exercises")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("""
                        INSERT INTO exercises (name, muscle_group, description) VALUES
                        ('Press de banca plano', 'Pecho', 'Empuje horizontal básico con barra'),
                        ('Aperturas con mancuernas', 'Pecho', 'Aislamiento de la zona pectoral'),
                        ('Dominadas en barra fija', 'Espalda', 'Tracción vertical para dorsales y espalda alta'),
                        ('Remo con barra', 'Espalda', 'Tracción horizontal para grosor de espalda'),
                        ('Sentadilla libre trasera', 'Piernas', 'Ejercicio compuesto principal para cuádriceps y glúteos'),
                        ('Prensa de piernas 45°', 'Piernas', 'Carga guiada de tren inferior'),
                        ('Press militar de hombros', 'Hombros', 'Empuje vertical para deltoides'),
                        ('Elevaciones laterales', 'Hombros', 'Aislamiento de cabeza lateral del hombro'),
                        ('Curl de bíceps con barra Z', 'Brazos', 'Flexión de codos para bíceps braquial'),
                        ('Fondos en paralelas', 'Brazos', 'Énfasis en tríceps y pectoral inferior'),
                        ('Plancha isométrica central', 'Core', 'Estabilidad del abdomen y zona media'),
                        ('Caminadora inclinada', 'Cardio', 'Resistencia aeróbica y calentamiento');
                    """);
                }
            }

            // Semillas de configuración
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM config")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("""
                        INSERT INTO config (key, value) VALUES
                        ('gym_name', 'GymDesk Fitness Club'),
                        ('tax_id', '20600011122'),
                        ('address', 'Av. Principal 123'),
                        ('phone', '987654321'),
                        ('email', 'contacto@gymdesk.com'),
                        ('currency', 'S/'),
                        ('last_backup_date', 'Nunca'),
                        ('receipt_footer', 'Gracias por entrenar con nosotros. Control interno no válido para SUNAT.');
                    """);
                }
            }

            // Semillas de usuarios iniciales (Admin y Recepción con PBKDF2)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String adminSalt = PasswordUtil.generateSalt();
                    String adminHash = PasswordUtil.hashPassword("admin123", adminSalt);
                    String recSalt = PasswordUtil.generateSalt();
                    String recHash = PasswordUtil.hashPassword("recepcion123", recSalt);
                    String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);

                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO users (username, password_hash, salt, full_name, role, active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setString(1, "admin");
                        ps.setString(2, adminHash);
                        ps.setString(3, adminSalt);
                        ps.setString(4, "Administrador del Sistema");
                        ps.setString(5, "ADMINISTRADOR");
                        ps.setInt(6, 1);
                        ps.setString(7, now);
                        ps.executeUpdate();

                        ps.setString(1, "recepcion");
                        ps.setString(2, recHash);
                        ps.setString(3, recSalt);
                        ps.setString(4, "Recepcionista de Turno");
                        ps.setString(5, "RECEPCIONISTA");
                        ps.setInt(6, 1);
                        ps.setString(7, now);
                        ps.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo iniciar la base de datos SQLite: " + e.getMessage(), e);
        }
    }

    private void migratePaymentsColumns(Statement stmt) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(payments)")) {
            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase());
            }
        }

        if (!columns.contains("original_amount")) stmt.execute("ALTER TABLE payments ADD COLUMN original_amount REAL DEFAULT 0.0;");
        if (!columns.contains("discount_applied")) stmt.execute("ALTER TABLE payments ADD COLUMN discount_applied REAL DEFAULT 0.0;");
        if (!columns.contains("promotion_name")) stmt.execute("ALTER TABLE payments ADD COLUMN promotion_name TEXT DEFAULT '';");
        if (!columns.contains("status")) stmt.execute("ALTER TABLE payments ADD COLUMN status TEXT DEFAULT 'Registrado';");
        if (!columns.contains("cancellation_reason")) stmt.execute("ALTER TABLE payments ADD COLUMN cancellation_reason TEXT DEFAULT '';");

        stmt.execute("UPDATE payments SET original_amount = amount WHERE original_amount = 0.0 OR original_amount IS NULL;");
        stmt.execute("UPDATE payments SET status = 'Registrado' WHERE status IS NULL OR status = '';");
    }

    private void migrateRoutinesColumns(Statement stmt) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(routines)")) {
            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase());
            }
        }

        if (!columns.contains("name")) stmt.execute("ALTER TABLE routines ADD COLUMN name TEXT DEFAULT '';");
        if (!columns.contains("goal")) stmt.execute("ALTER TABLE routines ADD COLUMN goal TEXT DEFAULT '';");
        if (!columns.contains("notes")) stmt.execute("ALTER TABLE routines ADD COLUMN notes TEXT DEFAULT '';");

        stmt.execute("UPDATE routines SET name = title WHERE (name = '' OR name IS NULL) AND title IS NOT NULL;");
        stmt.execute("UPDATE routines SET notes = details WHERE (notes = '' OR notes IS NULL) AND details IS NOT NULL;");
    }

    // ==========================================
    // SOCIOS
    // ==========================================

    @Override
    public List<Member> members() {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT id, document, full_name, phone, email, status, plan, start_date, end_date FROM members ORDER BY id DESC";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapMember(rs));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public Optional<Member> findMemberById(int id) {
        String sql = "SELECT id, document, full_name, phone, email, status, plan, start_date, end_date FROM members WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapMember(rs));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return Optional.empty();
    }

    @Override
    public void saveMember(Member m) {
        boolean isNew = (m.id() == 0);
        String sql = isNew
                ? "INSERT INTO members (document, full_name, phone, email, status, plan, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                : "UPDATE members SET document = ?, full_name = ?, phone = ?, email = ?, status = ?, plan = ?, start_date = ?, end_date = ? WHERE id = ?";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.document());
            ps.setString(2, m.fullName());
            ps.setString(3, m.phone());
            ps.setString(4, m.email());
            ps.setString(5, m.status());
            ps.setString(6, m.plan());
            ps.setString(7, m.startDate());
            ps.setString(8, m.endDate());

            if (!isNew) {
                ps.setInt(9, m.id());
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public void updateMemberPlanAndDates(int memberId, String planName, String startDate, String endDate, String status) {
        String sql = "UPDATE members SET plan = ?, start_date = ?, end_date = ?, status = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, planName);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            ps.setString(4, status);
            ps.setInt(5, memberId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public void deactivateMember(int id) {
        String sql = "UPDATE members SET status = 'Inactivo' WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public void deleteMember(int id) {
        deactivateMember(id);
    }

    // ==========================================
    // PLANES
    // ==========================================

    @Override
    public List<Plan> plans() {
        List<Plan> list = new ArrayList<>();
        String sql = "SELECT id, name, duration_days, price, description FROM plans ORDER BY price ASC";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapPlan(rs));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public Optional<Plan> findPlanById(int id) {
        String sql = "SELECT id, name, duration_days, price, description FROM plans WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPlan(rs));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return Optional.empty();
    }

    @Override
    public void savePlan(Plan plan) {
        String sql = "INSERT INTO plans (name, duration_days, price, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plan.name());
            ps.setInt(2, plan.durationDays());
            ps.setDouble(3, plan.price());
            ps.setString(4, plan.description());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public void updatePlan(Plan plan) {
        String sql = "UPDATE plans SET name = ?, duration_days = ?, price = ?, description = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plan.name());
            ps.setInt(2, plan.durationDays());
            ps.setDouble(3, plan.price());
            ps.setString(4, plan.description());
            ps.setInt(5, plan.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    // ==========================================
    // MEMBRESÍAS
    // ==========================================

    @Override
    public List<Membership> membershipsByMember(int memberId) {
        List<Membership> list = new ArrayList<>();
        String sql = """
            SELECT ms.id, ms.member_id, ms.plan_id, p.name AS plan_name, ms.start_date, ms.end_date, ms.price_paid, ms.status
            FROM memberships ms
            JOIN plans p ON p.id = ms.plan_id
            WHERE ms.member_id = ?
            ORDER BY ms.id DESC
        """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMembership(rs));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public Optional<Membership> findLatestMembership(int memberId) {
        String sql = """
            SELECT ms.id, ms.member_id, ms.plan_id, p.name AS plan_name, ms.start_date, ms.end_date, ms.price_paid, ms.status
            FROM memberships ms
            JOIN plans p ON p.id = ms.plan_id
            WHERE ms.member_id = ?
            ORDER BY ms.end_date DESC, ms.id DESC
            LIMIT 1
        """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapMembership(rs));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return Optional.empty();
    }

    @Override
    public void saveMembership(Membership m) {
        boolean isNew = (m.id() == 0);
        String sql = isNew
                ? "INSERT INTO memberships (member_id, plan_id, start_date, end_date, price_paid, status) VALUES (?, ?, ?, ?, ?, ?)"
                : "UPDATE memberships SET member_id = ?, plan_id = ?, start_date = ?, end_date = ?, price_paid = ?, status = ? WHERE id = ?";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, m.memberId());
            ps.setInt(2, m.planId());
            ps.setString(3, m.startDate());
            ps.setString(4, m.endDate());
            ps.setDouble(5, m.pricePaid());
            ps.setString(6, m.status());

            if (!isNew) {
                ps.setInt(7, m.id());
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    // ==========================================
    // OFERTAS Y PROMOCIONES
    // ==========================================

    @Override
    public List<Offer> offers() {
        List<Offer> list = new ArrayList<>();
        String sql = "SELECT id, name, discount_type, discount_value, valid_until, plan_id, active, description FROM offers ORDER BY active DESC, id DESC";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapOffer(rs));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public Optional<Offer> findOfferById(int id) {
        String sql = "SELECT id, name, discount_type, discount_value, valid_until, plan_id, active, description FROM offers WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapOffer(rs));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return Optional.empty();
    }

    @Override
    public void saveOffer(Offer offer) {
        boolean isNew = (offer.id() == 0);
        String sql = isNew
                ? "INSERT INTO offers (name, discount_type, discount_value, valid_until, plan_id, active, description) VALUES (?, ?, ?, ?, ?, ?, ?)"
                : "UPDATE offers SET name = ?, discount_type = ?, discount_value = ?, valid_until = ?, plan_id = ?, active = ?, description = ? WHERE id = ?";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, offer.name());
            ps.setString(2, offer.discountType());
            ps.setDouble(3, offer.discountValue());
            ps.setString(4, offer.validUntil());
            if (offer.planId() != null) {
                ps.setInt(5, offer.planId());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            ps.setInt(6, offer.active() ? 1 : 0);
            ps.setString(7, offer.description());

            if (!isNew) {
                ps.setInt(8, offer.id());
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public void toggleOfferStatus(int id, boolean active) {
        String sql = "UPDATE offers SET active = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    // ==========================================
    // PAGOS / COBROS
    // ==========================================

    @Override
    public List<Payment> payments() {
        List<Payment> list = new ArrayList<>();
        String sql = """
            SELECT p.id, p.member_id, m.full_name,
                   COALESCE(p.original_amount, p.amount) AS original_amount,
                   COALESCE(p.discount_applied, 0.0) AS discount_applied,
                   p.amount,
                   COALESCE(p.promotion_name, '') AS promotion_name,
                   p.method, p.concept, p.date, p.invoice_number,
                   COALESCE(p.status, 'Registrado') AS status,
                   COALESCE(p.cancellation_reason, '') AS cancellation_reason
            FROM payments p
            JOIN members m ON m.id = p.member_id
            ORDER BY p.id DESC
        """;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapPayment(rs));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public List<Payment> paymentsBetween(String startDate, String endDate) {
        List<Payment> list = new ArrayList<>();
        String sql = """
            SELECT p.id, p.member_id, m.full_name,
                   COALESCE(p.original_amount, p.amount) AS original_amount,
                   COALESCE(p.discount_applied, 0.0) AS discount_applied,
                   p.amount,
                   COALESCE(p.promotion_name, '') AS promotion_name,
                   p.method, p.concept, p.date, p.invoice_number,
                   COALESCE(p.status, 'Registrado') AS status,
                   COALESCE(p.cancellation_reason, '') AS cancellation_reason
            FROM payments p
            JOIN members m ON m.id = p.member_id
            WHERE date(p.date) >= date(?) AND date(p.date) <= date(?)
            ORDER BY p.id DESC
        """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPayment(rs));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public Optional<Payment> findPaymentById(int id) {
        String sql = """
            SELECT p.id, p.member_id, m.full_name,
                   COALESCE(p.original_amount, p.amount) AS original_amount,
                   COALESCE(p.discount_applied, 0.0) AS discount_applied,
                   p.amount,
                   COALESCE(p.promotion_name, '') AS promotion_name,
                   p.method, p.concept, p.date, p.invoice_number,
                   COALESCE(p.status, 'Registrado') AS status,
                   COALESCE(p.cancellation_reason, '') AS cancellation_reason
            FROM payments p
            JOIN members m ON m.id = p.member_id
            WHERE p.id = ?
        """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPayment(rs));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return Optional.empty();
    }

    @Override
    public void addPayment(int memberId, double originalAmount, double discountApplied, double finalAmount, String promotionName, String method, String concept) {
        String sql = """
            INSERT INTO payments (member_id, original_amount, discount_applied, amount, promotion_name, method, concept, date, invoice_number, status, cancellation_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Registrado', '')
        """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setDouble(2, originalAmount);
            ps.setDouble(3, discountApplied);
            ps.setDouble(4, finalAmount);
            ps.setString(5, promotionName != null ? promotionName : "");
            ps.setString(6, method);
            ps.setString(7, concept);
            ps.setString(8, LocalDate.now().toString());
            ps.setString(9, "BOL-" + (System.currentTimeMillis() % 1000000));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public void cancelPayment(int paymentId, String reason) {
        String sql = "UPDATE payments SET status = 'Anulado', cancellation_reason = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, paymentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    // ==========================================
    // CATÁLOGO DE EJERCICIOS
    // ==========================================

    @Override
    public List<Exercise> exercises() {
        List<Exercise> list = new ArrayList<>();
        String sql = "SELECT id, name, muscle_group, description FROM exercises ORDER BY muscle_group, name";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Exercise(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("muscle_group"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public void saveExercise(Exercise ex) {
        String sql = "INSERT INTO exercises (name, muscle_group, description) VALUES (?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ex.name());
            ps.setString(2, ex.muscleGroup());
            ps.setString(3, ex.description());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    // ==========================================
    // RUTINAS
    // ==========================================

    @Override
    public List<Routine> routines() {
        List<Routine> list = new ArrayList<>();
        String sql = """
            SELECT r.id, r.member_id, m.full_name,
                   COALESCE(NULLIF(r.name, ''), r.title, 'Rutina') AS name,
                   COALESCE(r.goal, 'Acondicionamiento') AS goal,
                   COALESCE(NULLIF(r.notes, ''), r.details, '') AS notes,
                   r.created_at
            FROM routines r
            JOIN members m ON m.id = r.member_id
            ORDER BY r.id DESC
        """;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int routineId = rs.getInt("id");
                List<RoutineExercise> exercises = loadRoutineExercises(conn, routineId);
                list.add(new Routine(
                        routineId,
                        rs.getInt("member_id"),
                        rs.getString("full_name"),
                        rs.getString("name"),
                        rs.getString("goal"),
                        rs.getString("notes"),
                        rs.getString("created_at"),
                        exercises
                ));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public List<Routine> routinesByMember(int memberId) {
        List<Routine> list = new ArrayList<>();
        String sql = """
            SELECT r.id, r.member_id, m.full_name,
                   COALESCE(NULLIF(r.name, ''), r.title, 'Rutina') AS name,
                   COALESCE(r.goal, 'Acondicionamiento') AS goal,
                   COALESCE(NULLIF(r.notes, ''), r.details, '') AS notes,
                   r.created_at
            FROM routines r
            JOIN members m ON m.id = r.member_id
            WHERE r.member_id = ?
            ORDER BY r.id DESC
        """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int routineId = rs.getInt("id");
                    List<RoutineExercise> exercises = loadRoutineExercises(conn, routineId);
                    list.add(new Routine(
                            routineId,
                            rs.getInt("member_id"),
                            rs.getString("full_name"),
                            rs.getString("name"),
                            rs.getString("goal"),
                            rs.getString("notes"),
                            rs.getString("created_at"),
                            exercises
                    ));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    private List<RoutineExercise> loadRoutineExercises(Connection conn, int routineId) throws SQLException {
        List<RoutineExercise> list = new ArrayList<>();
        String sql = "SELECT id, routine_id, exercise_name, series, repetitions, rest_seconds, notes FROM routine_exercises WHERE routine_id = ? ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, routineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RoutineExercise(
                            rs.getInt("id"),
                            rs.getInt("routine_id"),
                            rs.getString("exercise_name"),
                            rs.getInt("series"),
                            rs.getString("repetitions"),
                            rs.getInt("rest_seconds"),
                            rs.getString("notes")
                    ));
                }
            }
        }
        return list;
    }

    @Override
    public void saveRoutine(Routine r) {
        String sql = "INSERT INTO routines (member_id, name, goal, notes, created_at, title, details) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            int generatedRoutineId = 0;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, r.memberId());
                ps.setString(2, r.name());
                ps.setString(3, r.goal());
                ps.setString(4, r.notes());
                ps.setString(5, r.createdAt());
                ps.setString(6, r.name());
                ps.setString(7, r.notes());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedRoutineId = keys.getInt(1);
                    }
                }
            }

            if (r.exercises() != null && !r.exercises().isEmpty() && generatedRoutineId > 0) {
                String itemSql = "INSERT INTO routine_exercises (routine_id, exercise_name, series, repetitions, rest_seconds, notes) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ips = conn.prepareStatement(itemSql)) {
                    for (RoutineExercise re : r.exercises()) {
                        ips.setInt(1, generatedRoutineId);
                        ips.setString(2, re.exerciseName());
                        ips.setInt(3, re.series());
                        ips.setString(4, re.repetitions());
                        ips.setInt(5, re.restSeconds());
                        ips.setString(6, re.notes() != null ? re.notes() : "");
                        ips.addBatch();
                    }
                    ips.executeBatch();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    // ==========================================
    // ASISTENCIA
    // ==========================================

    @Override
    public List<Attendance> attendance() {
        List<Attendance> list = new ArrayList<>();
        String sql = """
            SELECT a.id, a.member_id, m.full_name, m.document, a.check_date, m.status, m.plan
            FROM attendance a
            JOIN members m ON m.id = a.member_id
            ORDER BY a.id DESC
        """;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapAttendance(rs));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public List<Attendance> attendanceByMember(int memberId) {
        List<Attendance> list = new ArrayList<>();
        String sql = """
            SELECT a.id, a.member_id, m.full_name, m.document, a.check_date, m.status, m.plan
            FROM attendance a
            JOIN members m ON m.id = a.member_id
            WHERE a.member_id = ?
            ORDER BY a.id DESC
        """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAttendance(rs));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public int countAttendanceBetween(String startDate, String endDate) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE date(check_date) >= date(?) AND date(check_date) <= date(?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public Attendance checkIn(int memberId) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String insertSql = "INSERT INTO attendance (member_id, check_date) VALUES (?, ?)";
        int newId = 0;

        try (Connection conn = connect()) {
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, memberId);
                ps.setString(2, timestamp);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        newId = keys.getInt(1);
                    }
                }
            }

            String querySql = """
                SELECT a.id, a.member_id, m.full_name, m.document, a.check_date, m.status, m.plan
                FROM attendance a
                JOIN members m ON m.id = a.member_id
                WHERE a.id = ?
            """;
            try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                ps.setInt(1, newId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapAttendance(rs);
                    }
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }

        throw new IllegalStateException("No se pudo recuperar el registro de ingreso recién creado.");
    }

    // ==========================================
    // CONFIGURACIÓN COMERCIAL
    // ==========================================

    @Override
    public GymConfig config() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT key, value FROM config";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                map.put(rs.getString("key"), rs.getString("value"));
            }
        } catch (SQLException e) {
            throw fail(e);
        }

        if (map.isEmpty()) {
            return GymConfig.defaultConfig();
        }

        return new GymConfig(
                map.getOrDefault("gym_name", "GymDesk Fitness Club"),
                map.getOrDefault("tax_id", "20600011122"),
                map.getOrDefault("address", "Av. Principal 123"),
                map.getOrDefault("phone", "987654321"),
                map.getOrDefault("email", "contacto@gymdesk.com"),
                map.getOrDefault("currency", "S/"),
                map.getOrDefault("receipt_footer", "Gracias por su preferencia. Control interno no válido para SUNAT.")
        );
    }

    @Override
    public void saveConfig(GymConfig c) {
        String sql = "INSERT OR REPLACE INTO config (key, value) VALUES (?, ?)";
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String[][] pairs = {
                        {"gym_name", c.gymName()},
                        {"tax_id", c.taxId()},
                        {"address", c.address()},
                        {"phone", c.phone()},
                        {"email", c.email()},
                        {"currency", c.currency()},
                        {"receipt_footer", c.receiptFooter()}
                };
                for (String[] pair : pairs) {
                    ps.setString(1, pair[0]);
                    ps.setString(2, pair[1] != null ? pair[1] : "");
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    // ==========================================
    // INDICADORES Y REPORTES
    // ==========================================

    @Override
    public int count(String table) {
        if (!ALLOWED_COUNT_TABLES.contains(table)) {
            throw new IllegalArgumentException("Tabla no permitida para conteo: " + table);
        }
        String sql = "SELECT COUNT(*) FROM " + table;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public double incomeThisMonth() {
        String sql = """
            SELECT COALESCE(SUM(amount), 0) FROM payments
            WHERE strftime('%Y-%m', date) = strftime('%Y-%m', 'now')
              AND (status IS NULL OR status != 'Anulado')
        """;
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    // ==========================================
    // USUARIOS Y ROLES
    // ==========================================

    @Override
    public List<User> users() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, salt, full_name, role, active, created_at FROM users ORDER BY id ASC;";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    @Override
    public Optional<User> findUserById(int id) {
        String sql = "SELECT id, username, password_hash, salt, full_name, role, active, created_at FROM users WHERE id = ?;";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findUserByUsername(String username) {
        if (username == null || username.trim().isBlank()) return Optional.empty();
        String sql = "SELECT id, username, password_hash, salt, full_name, role, active, created_at FROM users WHERE LOWER(username) = LOWER(?);";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return Optional.empty();
    }

    @Override
    public void saveUser(User user) {
        String sql = "INSERT INTO users (username, password_hash, salt, full_name, role, active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.username().trim());
            ps.setString(2, user.passwordHash());
            ps.setString(3, user.salt());
            ps.setString(4, user.fullName().trim());
            ps.setString(5, user.role());
            ps.setInt(6, user.active() ? 1 : 0);
            ps.setString(7, user.createdAt() != null ? user.createdAt() : LocalDateTime.now().format(DATE_TIME_FORMATTER));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public void updateUser(User user) {
        String sql = "UPDATE users SET full_name = ?, role = ? WHERE id = ?;";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.fullName().trim());
            ps.setString(2, user.role());
            ps.setInt(3, user.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public void updateUserPassword(int userId, String newHash, String newSalt) {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?;";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, newSalt);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public void toggleUserStatus(int id, boolean active) {
        String sql = "UPDATE users SET active = ? WHERE id = ?;";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    // ==========================================
    // COPIAS DE SEGURIDAD (RESPALDOS)
    // ==========================================

    @Override
    public String createBackup(File targetDirectory) {
        if (targetDirectory == null) {
            throw new IllegalArgumentException("El directorio de destino no puede ser nulo.");
        }
        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            throw new IllegalArgumentException("No se pudo crear el directorio de destino para el respaldo.");
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File backupFile = new File(targetDirectory, "gymdesk_backup_" + timestamp + ".db");
        String targetPath = backupFile.getAbsolutePath().replace('\\', '/');

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("VACUUM INTO '" + targetPath + "';");
        } catch (SQLException e) {
            throw fail(e);
        }

        String nowFormatted = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String updateDateSql = "INSERT INTO config (key, value) VALUES ('last_backup_date', ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value;";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(updateDateSql)) {
            ps.setString(1, nowFormatted);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }

        return backupFile.getAbsolutePath();
    }

    @Override
    public void restoreBackup(File backupFile) {
        if (backupFile == null || !backupFile.exists() || !backupFile.isFile()) {
            throw new IllegalArgumentException("El archivo de respaldo no existe o es inválido.");
        }

        // 1. Verificar integridad del archivo SQLite a restaurar
        String testUrl = "jdbc:sqlite:" + backupFile.getAbsolutePath().replace('\\', '/');
        try (Connection testConn = DriverManager.getConnection(testUrl);
             Statement stmt = testConn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("PRAGMA quick_check;")) {
                if (!rs.next() || !"ok".equalsIgnoreCase(rs.getString(1))) {
                    throw new IllegalArgumentException("El archivo seleccionado está dañado o no es una base de datos válida.");
                }
            }
            try (ResultSet tablesRs = stmt.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('members', 'users', 'plans');")) {
                if (!tablesRs.next() || tablesRs.getInt(1) < 2) {
                    throw new IllegalArgumentException("El archivo seleccionado no corresponde a un respaldo válido de GymDesk.");
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Error al verificar la base de datos de respaldo: " + e.getMessage(), e);
        }

        // 2. Extraer el path de la base de datos actual
        if (!url.startsWith("jdbc:sqlite:")) {
            throw new IllegalStateException("La restauración directa solo es soportada para conexiones jdbc:sqlite:<archivo>.");
        }
        String currentDbPath = url.substring("jdbc:sqlite:".length());
        File currentDbFile = new File(currentDbPath);

        try {
            if (currentDbFile.exists()) {
                File bak = new File(currentDbFile.getParentFile(), currentDbFile.getName() + ".pre_restore.bak");
                Files.copy(currentDbFile.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.copy(backupFile.toPath(), currentDbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new IllegalStateException("Error al sobreescribir la base de datos con el respaldo: " + e.getMessage(), e);
        }

        // 3. Re-inicializar para verificar llaves y consistencia
        initDatabase();
    }

    @Override
    public String getLastBackupDate() {
        String sql = "SELECT value FROM config WHERE key = 'last_backup_date';";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException ignored) {
        }
        return "Nunca";
    }

    // ==========================================
    // AUDITORÍA DE EVENTOS
    // ==========================================

    @Override
    public void addAuditLog(AuditLog log) {
        String sql = "INSERT INTO audit_logs (timestamp, username, action, details) VALUES (?, ?, ?, ?);";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, log.timestamp() != null ? log.timestamp() : LocalDateTime.now().format(DATE_TIME_FORMATTER));
            ps.setString(2, log.username() != null ? log.username() : "SISTEMA");
            ps.setString(3, log.action());
            ps.setString(4, log.details());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    @Override
    public List<AuditLog> auditLogs() {
        return recentAuditLogs(500);
    }

    @Override
    public List<AuditLog> recentAuditLogs(int limit) {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT id, timestamp, username, action, details FROM audit_logs ORDER BY id DESC LIMIT ?;";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AuditLog(
                            rs.getInt("id"),
                            rs.getString("timestamp"),
                            rs.getString("username"),
                            rs.getString("action"),
                            rs.getString("details")
                    ));
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    // ==========================================
    // MAPEOS
    // ==========================================

    private Member mapMember(ResultSet rs) throws SQLException {
        return new Member(
                rs.getInt("id"),
                rs.getString("document"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getString("plan"),
                rs.getString("start_date"),
                rs.getString("end_date")
        );
    }

    private Plan mapPlan(ResultSet rs) throws SQLException {
        return new Plan(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("duration_days"),
                rs.getDouble("price"),
                rs.getString("description")
        );
    }

    private Membership mapMembership(ResultSet rs) throws SQLException {
        return new Membership(
                rs.getInt("id"),
                rs.getInt("member_id"),
                rs.getInt("plan_id"),
                rs.getString("plan_name"),
                rs.getString("start_date"),
                rs.getString("end_date"),
                rs.getDouble("price_paid"),
                rs.getString("status")
        );
    }

    private Offer mapOffer(ResultSet rs) throws SQLException {
        int planIdRaw = rs.getInt("plan_id");
        Integer planId = rs.wasNull() ? null : planIdRaw;
        return new Offer(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("discount_type"),
                rs.getDouble("discount_value"),
                rs.getString("valid_until"),
                planId,
                rs.getInt("active") == 1,
                rs.getString("description")
        );
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        return new Payment(
                rs.getInt("id"),
                rs.getInt("member_id"),
                rs.getString("full_name"),
                rs.getDouble("original_amount"),
                rs.getDouble("discount_applied"),
                rs.getDouble("amount"),
                rs.getString("promotion_name"),
                rs.getString("method"),
                rs.getString("concept"),
                rs.getString("date"),
                rs.getString("invoice_number"),
                rs.getString("status"),
                rs.getString("cancellation_reason")
        );
    }

    private Attendance mapAttendance(ResultSet rs) throws SQLException {
        return new Attendance(
                rs.getInt("id"),
                rs.getInt("member_id"),
                rs.getString("full_name"),
                rs.getString("document"),
                rs.getString("check_date"),
                rs.getString("status"),
                rs.getString("plan")
        );
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("salt"),
                rs.getString("full_name"),
                rs.getString("role"),
                rs.getInt("active") == 1,
                rs.getString("created_at")
        );
    }

    private RuntimeException fail(SQLException e) {
        return new IllegalStateException("Error de base de datos: " + e.getMessage(), e);
    }
}
