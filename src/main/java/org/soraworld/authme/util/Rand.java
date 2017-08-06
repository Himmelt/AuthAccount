package org.soraworld.authme.util;

import java.util.Random;

public class Rand {

    private static final String CHARS = "qLpAaKlSzJmDoHkFjGuMiZwNnXsBhCxVyQePbWdOgEcItRrUvTfY";
    private static final Random RANDOM = new Random();

    public static String randString(int count) {
        StringBuilder builder = new StringBuilder();
        RANDOM.setSeed(System.currentTimeMillis());
        while (count-- != 0) {
            builder.append(CHARS.charAt(RANDOM.nextInt(52)));
        }
        return builder.toString();
    }
}
