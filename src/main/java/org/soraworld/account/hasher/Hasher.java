package org.soraworld.account.hasher;

public interface Hasher {
    String hash(String password) throws Exception;

    boolean checkPassword(String hash, String input) throws Exception;
}
