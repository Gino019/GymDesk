package com.gymdesk.domain;

import java.time.LocalDate;

/**
 * Representa una oferta o promoción de descuento en el gimnasio.
 * El modelo es mixto: el sistema evalúa compatibilidad y recepción decide cuál aplicar.
 */
public record Offer(
        int id,
        String name,
        String discountType,  // "PERCENTAGE" (Porcentual) o "FIXED" (Monto fijo)
        double discountValue, // Ej. 15.0 para 15% o 20.0 para S/ 20.00
        String validUntil,    // Fecha límite ISO (YYYY-MM-DD) o null si es permanente
        Integer planId,       // ID del plan específico al que aplica, o null si aplica a cualquier plan
        boolean active,
        String description
) {

    /**
     * Verifica si la promoción está activa y dentro de su período de vigencia.
     */
    public boolean isValid() {
        if (!active) {
            return false;
        }
        if (validUntil != null && !validUntil.trim().isBlank()) {
            try {
                LocalDate expiration = LocalDate.parse(validUntil.trim());
                if (expiration.isBefore(LocalDate.now())) {
                    return false;
                }
            } catch (Exception ignored) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calcula el monto a descontar sobre un importe base.
     */
    public double calculateDiscount(double originalAmount) {
        if (originalAmount <= 0) {
            return 0.0;
        }
        if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
            double discount = originalAmount * (discountValue / 100.0);
            return Math.round(discount * 100.0) / 100.0;
        } else {
            return Math.min(originalAmount, Math.max(0.0, discountValue));
        }
    }
}

