package org.soraworld.account.util;

public final class Pswd {
    private static final PasswordEncoder encoder = new PasswordEncoder(PasswordEncoder.BCryptVersion.$2B);

    public static String encode(String rawPswd) {
        return encoder.encode(rawPswd);
    }

    public static boolean matches(String plainPswd, String hashedPswd) {
        return encoder.matches(plainPswd, hashedPswd);
    }
}
