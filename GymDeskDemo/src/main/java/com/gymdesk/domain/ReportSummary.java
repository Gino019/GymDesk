package com.gymdesk.domain;

import java.util.List;

/**
 * Resumen consolidado para reportes de operaciones y caja por período.
 */
public record ReportSummary(
        String startDate,
        String endDate,
        double grossIncome,          // Ingresos netos efectivos (excluyendo anulados)
        double totalDiscounts,       // Total de descuentos otorgados
        int totalPaymentsCount,      // Total de pagos registrados
        int activePaymentsCount,     // Pagos vigentes
        int cancelledPaymentsCount,  // Pagos anulados
        int totalAttendancesCount,   // Ingresos a las instalaciones en el rango
        List<Payment> payments
) {}

