package com.gymdesk.domain;

/**
 * Representa a un socio registrado en el gimnasio.
 */
public record Member(
        int id,
        String document,
        String fullName,
        String phone,
        String email,
        String status,
        String plan,
        String startDate,
        String endDate) {
    public boolean isActive() {
        return "Activo".equalsIgnoreCase(status);
    }
}
