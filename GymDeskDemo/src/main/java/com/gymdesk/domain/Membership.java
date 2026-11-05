package com.gymdesk.domain;

/**
 * Representa un período de membresía contratado por un socio.
 * Permite conservar el historial completo de inscripciones y renovaciones.
 */
public record Membership(
        int id,
        int memberId,
        int planId,
        String planName,
        String startDate,
        String endDate,
        double pricePaid,
        String status
) {
    public boolean isActive() {
        return "Activa".equalsIgnoreCase(status);
    }

    public boolean isExpired() {
        return "Vencida".equalsIgnoreCase(status);
    }
}

