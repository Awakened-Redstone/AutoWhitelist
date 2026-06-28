package com.awakenedredstone.autowhitelist.security;

import java.security.SecureRandom;
import java.util.Base64;

public class SecureGenerator {
    /**
     * Generates a 40 character random string from a set of 30 bytes generated with {@link SecureRandom}
     * <br/>
     * The string can be converted back to bytes by using {@link Base64.Decoder#decode(String)}
     *
     * @return A random Base64 encoded 40 character string
     */
    public static String generateSecret() {
        byte[] secretBytes = new byte[30];
        SecureRandom random = new SecureRandom();
        random.nextBytes(secretBytes);
        return Base64.getEncoder().encodeToString(secretBytes);
    }
}
