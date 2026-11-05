package com.gymdesk.service;

import com.gymdesk.domain.Attendance;

/**
 * Resultado estructurado del registro de ingreso de un socio a las instalaciones.
 */
public record CheckInResult(
        Attendance attendance,
        boolean isExpiredOrInactive,
        String message
) {}

