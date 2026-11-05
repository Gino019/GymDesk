package com.gymdesk.domain;

/**
 * Representa un usuario del sistema GymDesk con credenciales y rol asignado.
 */
public record User(
        int id,
        String username,
        String passwordHash,
        String salt,
        String fullName,
        String role,
        boolean active,
        String createdAt
) {
    public static final String ROLE_ADMIN = "ADMINISTRADOR";
    public static final String ROLE_RECEPTIONIST = "RECEPCIONISTA";

    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public boolean isReceptionist() {
        return ROLE_RECEPTIONIST.equalsIgnoreCase(role);
    }
}

