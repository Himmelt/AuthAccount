package org.soraworld.authme;

import com.google.common.primitives.Longs;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.api.entity.living.player.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class Account {

    private final UUID uuid;

    private String username;
    private String passwordHash;

    private String ip;
    private Timestamp timestamp;

    private String email;

    private boolean loggedIn;

    public Account(Player player, String password) {
        this(player.getUniqueId(), player.getName(), password, "invalid");
        byte[] ips = player.getConnection().getAddress().getAddress().getAddress();
        if (ips != null && ips.length == 4) {
            this.ip = ips[0] + "." + ips[1] + "." + ips[2] + "." + ips[3];
        } else {
            this.ip = "invalid";
        }
    }

    //new account
    public Account(UUID uuid, String username, String password, String ip) {
        this.uuid = uuid;
        this.username = username;
        this.passwordHash = password;

        this.ip = ip;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    //existing account
    public Account(ResultSet resultSet) throws SQLException {
        //uuid in binary format
        byte[] uuidBytes = resultSet.getBytes(2);

        byte[] mostBits = ArrayUtils.subarray(uuidBytes, 0, 8);
        byte[] leastBits = ArrayUtils.subarray(uuidBytes, 8, 16);

        long mostByte = Longs.fromByteArray(mostBits);
        long leastByte = Longs.fromByteArray(leastBits);

        this.uuid = new UUID(mostByte, leastByte);
        this.username = resultSet.getString(3);
        this.passwordHash = resultSet.getString(4);

        this.ip = resultSet.getString(5);
        this.timestamp = resultSet.getTimestamp(6);

        this.email = resultSet.getString(7);
    }

    public boolean checkPassword(Authme plugin, String userInput) throws Exception {
        return plugin.getHasher().checkPassword(passwordHash, userInput);
    }

    public UUID uuid() {
        return uuid;
    }

    public String username() {
        return username;
    }

    /* package */
    String getPassword() {
        return passwordHash;
    }

    public synchronized void setUsername(String username) {
        this.username = username;
    }

    public synchronized void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public synchronized void setIp(String ip) {
        this.ip = ip;
    }

    public synchronized void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public synchronized String ip() {
        return ip;
    }

    public synchronized Timestamp getTimestamp() {
        return timestamp;
    }

    public synchronized String getEmail() {
        return email;
    }

    public synchronized void setEmail(String email) {
        this.email = email;
    }

    //these methods have to thread-safe as they will be accessed
    //through Async (PlayerChatEvent/LoginTask) and sync methods
    public synchronized boolean isLoggedIn() {
        return loggedIn;
    }

    public synchronized void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}
