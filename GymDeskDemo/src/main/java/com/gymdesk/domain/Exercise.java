package com.gymdesk.domain;

/**
 * Representa un ejercicio dentro del catálogo del gimnasio.
 */
public record Exercise(
        int id,
        String name,
        String muscleGroup, // "Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Core", "Cardio"
        String description
) {}

