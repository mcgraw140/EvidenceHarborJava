package com.evidenceharbor.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtils {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH  = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtils() {}

    /** Hash a plaintext password. Returns a storable string: base64(salt):base64(hash) */
    public static String hash(String password) {
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt);
        return Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /** Verify a plaintext password against a stored hash string. */
    public static boolean verify(String password, String stored) {
        if (stored == null || !stored.contains(":")) return false;
        try {
            String[] parts = stored.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            byte[] actual = pbkdf2(password.toCharArray(), salt);
            return constantTimeEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] result = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /** Constant-time comparison to prevent timing attacks. */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= (a[i] ^ b[i]);
        return diff == 0;
    }
}
