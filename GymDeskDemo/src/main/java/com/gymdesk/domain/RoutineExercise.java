package com.gymdesk.domain;

/**
 * Representa la asignación de un ejercicio específico con series, repeticiones y descanso dentro de una rutina.
 */
public record RoutineExercise(
        int id,
        int routineId,
        String exerciseName,
        int series,
        String repetitions, // Ej. "10-12" o "15" o "Al fallo"
        int restSeconds,    // Ej. 60 o 90
        String notes
) {}

