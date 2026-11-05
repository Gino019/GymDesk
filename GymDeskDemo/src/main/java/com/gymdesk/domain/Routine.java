package com.gymdesk.domain;

import java.util.List;

/**
 * Representa una rutina o plan de entrenamiento asignado a un socio.
 */
public record Routine(
        int id,
        int memberId,
        String memberName,
        String name,
        String goal,
        String notes,
        String createdAt,
        List<RoutineExercise> exercises
) {}

