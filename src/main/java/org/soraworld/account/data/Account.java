package org.soraworld.account.data;

import org.mindrot.jbcrypt.BCrypt;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.soraworld.account.util.Hash.hash;

public class Account implements DataManipulator<Account, Account.Immutable> {

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

    private static final DataQuery IP = DataQuery.of("ip");
    private static final DataQuery PASSWORD = DataQuery.of("password");
    private static final DataQuery REGISTERED = DataQuery.of("registered");
    private static final DataQuery EMAIL = DataQuery.of("email");
    private static final DataQuery TIME = DataQuery.of("timestamp");

    public Account(Player player, String password) {
        this(player.getUniqueId(), player.getName(), password, 0);// TODO ip 0 ??? regip ? lastip ?
        this.lastIP = player.getConnection().getAddress().getAddress().hashCode();
    }

    //new account
    public Account(UUID uuid, String username, String password, int ip) {
        this.regIP = ip;
        this.uuid = uuid;
        this.username = username;
        this.password = hash(password);
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

    public Account(Immutable account) {
        this.uuid = account.uuid;
        this.username = account.username;
        this.password = account.password;
        this.registered = account.registered;
        this.email = account.email;
        this.regIP = account.ip;
        this.timestamp = account.timestamp;
        this.online = account.online;
    }

    public Account() {
    }

    public Account(User user, Account acc) {
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

    public boolean checkPassword(String password) {
        return BCrypt.checkpw(password, this.password);
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

    public synchronized void setPassword(String pswd) {
        this.password = hash(pswd);
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

    public synchronized void setOnline(boolean online) {
        this.online = online;
    }

    public Optional<Account> fill(DataHolder holder, MergeFunction overlap) {
        Account account = overlap.merge(this, holder.get(Account.class).orElse(null));
        // TODO copy
        if (holder instanceof User) {
            account.uuid = ((User) holder).getUniqueId();
            account.username = ((User) holder).getName();
        }
        return Optional.of(this);
    }

    public void reset() {
        registered = false;
        online = false;
        password = "";
        email = "";
        regIP = 0;
        timestamp = null;
    }

    public Optional<Account> from(DataContainer container) {
        if (container.contains(DataQuery.of("username"))) {
            reset();
            return Optional.of(this);
        } else return Optional.empty();
    }

    public <E> Account set(Key<? extends BaseValue<E>> key, E value) {
        return this;
    }

    public <E> Optional<E> get(Key<? extends BaseValue<E>> key) {
        return Optional.empty();
    }

    public <E, V extends BaseValue<E>> Optional<V> getValue(Key<V> key) {
        return Optional.empty();
    }

    public boolean supports(Key<?> key) {
        return false;
    }

    public Account copy() {
        return new Account(this);
    }

    public Set<Key<?>> getKeys() {
        return new HashSet<>();
    }

    public Set<ImmutableValue<?>> getValues() {
        return new HashSet<>();
    }

    public Immutable asImmutable() {
        return new Immutable(this);
    }

    public int getContentVersion() {
        return 0;
    }

    public DataContainer toContainer() {
        // TODO 是否只有在存储数据到文件时才调用
        return DataContainer.createNew()
                .set(IP, regIP)
                .set(EMAIL, email)
                .set(PASSWORD, password)
                .set(REGISTERED, registered);
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

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public static class Immutable implements ImmutableDataManipulator<Immutable, Account> {

        private UUID uuid;
        private int ip = 0;
        private String email = "";
        private String username = "";
        private String password = "";
        // TODO multiple servers online status
        private boolean online;
        private boolean registered = false;
        private Timestamp timestamp;

        public Immutable(Account account) {
            this.ip = account.regIP;
            this.email = account.email;
            this.password = account.password;
            this.online = account.online;
        }

        public Account asMutable() {
            return new Account(this);
        }

        public int getContentVersion() {
            return 0;
        }

        public DataContainer toContainer() {
            return asMutable().toContainer();
        }

        public <E> Optional<E> get(Key<? extends BaseValue<E>> key) {
            return Optional.empty();
        }

        public <E, V extends BaseValue<E>> Optional<V> getValue(Key<V> key) {
            return Optional.empty();
        }

        public boolean supports(Key<?> key) {
            return false;
        }

        public Set<Key<?>> getKeys() {
            return new HashSet<>();
        }

        public Set<ImmutableValue<?>> getValues() {
            return new HashSet<>();
        }
    }

    public static class Builder implements DataManipulatorBuilder<Account, Immutable> {
        public Account create() {
            return new Account();
        }

        public Optional<Account> createFrom(DataHolder holder) {
            return create().fill(holder);
        }

        public Optional<Account> build(DataView con) throws InvalidDataException {
            Account account = new Account();
            con.getInt(IP).ifPresent(s -> account.regIP = s);
            con.getString(EMAIL).ifPresent(s -> account.email = s);
            con.getString(PASSWORD).ifPresent(s -> account.password = s);
            con.getBoolean(REGISTERED).ifPresent(b -> account.registered = b);
            return Optional.of(account);
        }
    }
}
