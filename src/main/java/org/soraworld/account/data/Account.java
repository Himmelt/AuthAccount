package org.soraworld.account.data;

import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.util.IPUtil;
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Account implements DataManipulator<Account, Account.Immutable> {

    private int uid;
    private UUID uuid;
    private String ip;
    private String email;
    private String username;
    private String password;
    private boolean online;
    private Timestamp timestamp;

    private static final DataQuery IP = DataQuery.of("ip");
    private static final DataQuery UID = DataQuery.of("uid");
    private static final DataQuery UUID = DataQuery.of("uuid");
    private static final DataQuery EMAIL = DataQuery.of("email");
    private static final DataQuery PASSWORD = DataQuery.of("password");

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

    public Account(Account other) {

    }

    public boolean checkPassword(AccountManager manager, String userInput) {
        return AccountManager.hasher.checkPassword(passwordHash, userInput);
    }

    public UUID uuid() {
        return uuid;
    }

    public String username() {
        return username;
    }

    /* package */
    String getPassword() {
        return password;
    }

    public synchronized void setUsername(String username) {
        this.username = username;
    }

    public synchronized void setPasswordHash(String hash) {
        this.password = hash;
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

    public Optional<Account> fill(DataHolder dataHolder, MergeFunction overlap) {
        Account account = overlap.merge(this, dataHolder.get(Account.class).orElse(null));
        // TODO copy
        return Optional.of(this);
    }

    private void reset() {
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
        return DataContainer.createNew();
    }

    public static class Immutable implements ImmutableDataManipulator<Immutable, Account> {

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

        public Optional<Account> createFrom(DataHolder dataHolder) {
            return Optional.empty();
        }

        public Optional<Account> build(DataView container) throws InvalidDataException {
            return Optional.empty();
        }
    }
}
