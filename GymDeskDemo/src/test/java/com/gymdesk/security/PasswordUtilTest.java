package com.gymdesk.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    @DisplayName("generateSalt produce cadenas hexadecimales únicas de 32 caracteres (16 bytes)")
    void shouldGenerateUniqueHexSalt() {
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();

        assertNotNull(salt1);
        assertNotNull(salt2);
        assertEquals(32, salt1.length());
        assertEquals(32, salt2.length());
        assertNotEquals(salt1, salt2, "Dos sales generadas deben ser pseudoaleatorias y distintas");
    }

    @Test
    @DisplayName("hashPassword genera hashes reproducibles con la misma sal")
    void shouldGenerateConsistentHashForSamePasswordAndSalt() {
        String salt = PasswordUtil.generateSalt();
        String hash1 = PasswordUtil.hashPassword("secret123", salt);
        String hash2 = PasswordUtil.hashPassword("secret123", salt);

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "Hash SHA-256 codificado en hex debe tener 64 caracteres (256 bits)");
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("verifyPassword valida contraseñas correctas y rechaza incorrectas")
    void shouldVerifyPasswordsCorrectly() {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword("admin2026", salt);

        assertTrue(PasswordUtil.verifyPassword("admin2026", salt, hash), "Debe validar contraseña correcta");
        assertFalse(PasswordUtil.verifyPassword("wrongpass", salt, hash), "Debe rechazar contraseña incorrecta");
        assertFalse(PasswordUtil.verifyPassword("", salt, hash), "Debe rechazar contraseña vacía");
        assertFalse(PasswordUtil.verifyPassword(null, salt, hash), "Debe rechazar null");
    }
}

