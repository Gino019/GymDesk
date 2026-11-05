package com.gymdesk.domain;

/**
 * Representa un pago registrado por concepto de membresía u otro servicio.
 * Almacena el desglose inmutable del cobro (precio base, descuento aplicado,
 * precio final y promoción utilizada) para preservar la auditoría histórica.
 */
public record Payment(
        int id,
        int memberId,
        String memberName,
        double originalAmount,
        double discountApplied,
        double amount, // Monto final efectivamente cobrado
        String promotionName,
        String method,
        String concept,
        String date,
        String invoiceNumber,
        String status, // "Registrado", "Anulado"
        String cancellationReason
) {
    public boolean isCancelled() {
        return "Anulado".equalsIgnoreCase(status);
    }
}
