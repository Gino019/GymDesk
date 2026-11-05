package com.gymdesk.domain;

/**
 * Registro inmutable de auditoría para el seguimiento de eventos críticos del sistema.
 */
public record AuditLog(
        int id,
        String timestamp,
        String username,
        String action,
        String details
) {
    public static final String ACTION_LOGIN = "INICIO_SESION";
    public static final String ACTION_LOGOUT = "CIERRE_SESION";
    public static final String ACTION_CREATE_MEMBER = "CREAR_SOCIO";
    public static final String ACTION_UPDATE_MEMBER = "ACTUALIZAR_SOCIO";
    public static final String ACTION_DEACTIVATE_MEMBER = "BAJA_SOCIO";
    public static final String ACTION_RENEW_MEMBERSHIP = "RENOVAR_MEMBRESIA";
    public static final String ACTION_PAYMENT = "REGISTRAR_COBRO";
    public static final String ACTION_CANCEL_PAYMENT = "ANULAR_COBRO";
    public static final String ACTION_BACKUP_CREATE = "CREAR_RESPALDO";
    public static final String ACTION_BACKUP_RESTORE = "RESTAURAR_RESPALDO";
    public static final String ACTION_CONFIG_UPDATE = "MODIFICAR_CONFIGURACION";
    public static final String ACTION_USER_CREATE = "CREAR_USUARIO";
    public static final String ACTION_USER_TOGGLE = "CAMBIAR_ESTADO_USUARIO";
    public static final String ACTION_PASSWORD_CHANGE = "CAMBIO_CONTRASENA";
}

