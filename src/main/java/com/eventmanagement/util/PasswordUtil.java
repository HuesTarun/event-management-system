package com.eventmanagement.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtil {

    // Cost factor: 12 rounds is the industry standard for secure hashing.
    private static final int BCRYPT_COST = 12;


    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray());
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || plainPassword.isEmpty() || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(
                    plainPassword.toCharArray(),
                    hashedPassword.toCharArray()
            );
            return result.verified;
        } catch (Exception e) {
            System.err.println("[PasswordUtil] Error verifying password: " + e.getMessage());
            return false;
        }
    }
}
