package org.soraworld.authme;

import org.soraworld.authme.util.IPUtil;
import org.spongepowered.api.entity.living.player.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class Account {

    private final String uuid;

    private String username;
    private String passwordHash;

    private String ip;
    private Timestamp timestamp;

    private String email;

    private boolean online;

    public Account(Player player, String password) {
        this(player.getUniqueId(), player.getName(), password, "invalid-ip");
        this.ip = IPUtil.getPlayerIP(player);
    }

    //new account
    public Account(UUID uuid, String username, String password, String ip) {
        this.uuid = uuid.toString();
        this.username = username;
        this.passwordHash = password;

        this.ip = ip;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    //existing account
    public Account(ResultSet resultSet) throws SQLException {
        //uuid in string format
        this.uuid = resultSet.getString(2);
        this.username = resultSet.getString(3);
        this.passwordHash = resultSet.getString(4);
        this.ip = resultSet.getString(5);
        this.timestamp = resultSet.getTimestamp(6);
        this.email = resultSet.getString(7);
    }

    public boolean checkPassword(Authme plugin, String userInput) throws Exception {
        return plugin.getHasher().checkPassword(passwordHash, userInput);
    }

    public String uuid() {
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
    public synchronized boolean isOnline() {
        return online;
    }

    public synchronized void setOnline(boolean online) {
        this.online = online;
    }
}
