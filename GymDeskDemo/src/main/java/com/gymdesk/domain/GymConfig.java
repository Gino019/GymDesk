package com.gymdesk.domain;

/**
 * Representa los parámetros y datos comerciales de la instalación del gimnasio.
 * Permite que GymDesk sea comercializable sin codificar datos fijos.
 */
public record GymConfig(
        String gymName,
        String taxId,        // RUC / DNI o identificación comercial
        String address,
        String phone,
        String email,
        String currency,     // Ej. "S/" o "$"
        String receiptFooter
) {
    public static GymConfig defaultConfig() {
        return new GymConfig(
                "GymDesk Fitness Club",
                "20600011122",
                "Av. Principal 123",
                "987654321",
                "contacto@gymdesk.com",
                "S/",
                "Gracias por entrenar con nosotros. Documento de control interno no válido para SUNAT."
        );
    }
}

