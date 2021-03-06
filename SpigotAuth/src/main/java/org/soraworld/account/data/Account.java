package org.soraworld.account.data;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import static org.soraworld.account.util.Pswd.encode;
import static org.soraworld.account.util.Pswd.matches;

public class Account {

    private UUID uuid;
    private String username = "";
    // TODO multiple servers online status
    private boolean online = false;

    private int regIP = 0;
    private int lastIP = 0;
    private String email = "";
    private String password = "";
    private boolean registered = false;
    private Timestamp timestamp;

    public Account(Player player, String password) {
        this(player.getUniqueId(), player.getName(), password, 0);// TODO ip 0 ??? regip ? lastip ?
        this.lastIP = player.getAddress().getAddress().hashCode();
    }

    //new account
    public Account(UUID uuid, String username, String password, int ip) {
        this.regIP = ip;
        this.uuid = uuid;
        this.username = username;
        this.password = encode(password);
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    //existing account
    public Account(ResultSet resultSet) throws SQLException {
        //uuid in string format
        this.registered = true;
        this.password = resultSet.getString(4);
        this.regIP = resultSet.getInt(5);
        this.timestamp = resultSet.getTimestamp(6);
        this.email = resultSet.getString(7);
    }

    public Account(Account account) {
        this.uuid = account.uuid;
        this.username = account.username;
        this.password = account.password;
        this.registered = account.registered;
        this.email = account.email;
        this.regIP = account.regIP;
        this.timestamp = account.timestamp;
        this.online = account.online;
    }

    public Account() {
    }

    public Account(OfflinePlayer user, Account acc) {
        this.uuid = user.getUniqueId();
        this.username = user.getName();
        this.password = acc.password;
        this.email = acc.email;
        this.registered = true;
        this.regIP = acc.regIP;
        this.timestamp = acc.timestamp;
    }

    public Account(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean checkPassword(String plainPswd) {
        return matches(plainPswd, this.password);
    }

    public UUID uuid() {
        return uuid;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public synchronized void setUsername(String username) {
        this.username = username;
    }

    public Account setPassword(String pswd) {
        this.password = encode(pswd);
        return this;
    }

    public synchronized void setIp(int ip) {
        this.regIP = ip;
    }

    public synchronized void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public synchronized int ip() {
        return regIP;
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
    public synchronized boolean offline() {
        return !online;
    }

    public synchronized boolean online() {
        return online;
    }

    public synchronized Account setOnline(boolean online) {
        this.online = online;
        return this;
    }

    public void reset() {
        registered = false;
        online = false;
        password = "";
        email = "";
        regIP = 0;
        timestamp = null;
    }


    public Account copy() {
        return new Account(this);
    }

    public synchronized void sync(Account account) {
        this.uuid = account.uuid;
        this.username = account.username;
        this.registered = account.registered;
        this.online = account.online;
        if (account.regIP != 0) this.regIP = account.regIP;
        if (account.email != null && !account.email.isEmpty()) this.email = account.email;
        if (account.password != null && !account.password.isEmpty()) this.password = account.password;
        if (account.timestamp != null) this.timestamp = account.timestamp;
    }

    public boolean isRegistered() {
        return registered && password != null && !password.isEmpty();
    }

    public Account setRegistered(boolean registered) {
        this.registered = registered;
        return this;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }
}
