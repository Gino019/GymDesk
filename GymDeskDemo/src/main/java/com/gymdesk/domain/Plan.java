package com.gymdesk.domain;

/**
 * Representa un tipo de plan o membresía ofrecido por el gimnasio.
 */
public record Plan(
        int id,
        String name,
        int durationDays,
        double price,
        String description
) {}
