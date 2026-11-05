package com.gymdesk.domain;

/**
 * Representa el registro de ingreso de un socio al gimnasio.
 */
public record Attendance(
        int id,
        int memberId,
        String memberName,
        String memberDocument,
        String checkDate,
        String statusAtCheckIn, // "Activo", "Vencido", "Inactivo"
        String planName
) {
    public boolean isExpiredOrInactive() {
        return !"Activo".equalsIgnoreCase(statusAtCheckIn);
    }
}

