package org.soraworld.account.util;

import org.mindrot.jbcrypt.BCrypt;

public final class Hash {
    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean check(String hash, String input) {
        return BCrypt.checkpw(input, hash);
    }
}
