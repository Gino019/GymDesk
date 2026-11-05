package com.gymdesk.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

/**
 * Utilidad criptográfica para derivación segura de contraseñas mediante PBKDF2 con HmacSHA256 y sal (salt).
 * Cumple con los lineamientos de almacenamiento seguro sin contraseñas en texto plano.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
        // Clase de utilidades no instanciable
    }

    /**
     * Genera una sal criptográfica pseudoaleatoria codificada en hexadecimal.
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    /**
     * Deriva el hash criptográfico seguro de la contraseña utilizando la sal proporcionada.
     */
    public static String hashPassword(String password, String saltHex) {
        Objects.requireNonNull(password, "La contraseña no puede ser nula.");
        Objects.requireNonNull(saltHex, "La sal no puede ser nula.");

        byte[] salt = fromHex(saltHex);
        char[] passwordChars = password.toCharArray();

        try {
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return toHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Error al derivar el hash seguro de contraseña: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica en tiempo constante si la contraseña ingresada coincide con el hash esperado.
     */
    public static boolean verifyPassword(String inputPassword, String saltHex, String expectedHashHex) {
        if (inputPassword == null || saltHex == null || expectedHashHex == null) {
            return false;
        }
        try {
            String computedHash = hashPassword(inputPassword, saltHex);
            byte[] computedBytes = fromHex(computedHash);
            byte[] expectedBytes = fromHex(expectedHashHex);
            return MessageDigest.isEqual(computedBytes, expectedBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Cadena hexadecimal inválida.");
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}

