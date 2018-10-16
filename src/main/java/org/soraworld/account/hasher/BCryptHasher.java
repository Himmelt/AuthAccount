package org.soraworld.account.hasher;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptHasher implements Hasher {
    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean checkPassword(String hash, String input) {
        return BCrypt.checkpw(input, hash);
    }
}
