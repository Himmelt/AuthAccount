package org.soraworld.account.data;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.manager.DatabaseSetting;
import org.soraworld.violet.util.ChatColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.sql.SqlService;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Database {

    public static final String USERS_TABLE = "accounts";

    //this cache is thread-safe
    private final ConcurrentHashMap<UUID, Account> cache = new ConcurrentHashMap<>();

    private final String jdbcUrl;
    private final String username;
    private final String password;

    private SqlService sql;

    private final AccountManager manager;

    public Database(AccountManager manager, Path path) {
        this.manager = manager;

        String pluginId = manager.getPlugin().getId();

        DatabaseSetting dbCfg = manager.databaseSetting;

        if (dbCfg.isMySQL()) {
            this.username = dbCfg.username;
            this.password = dbCfg.password;
        } else {
            this.username = "";
            this.password = "";
        }

        String storagePath = dbCfg.path.replace("%DIR%", path.normalize().toString());

        StringBuilder sqlURL = new StringBuilder("jdbc:").append(dbCfg.type.toLowerCase()).append("://");
        if (dbCfg.isMySQL()) {
            //jdbc:<engine>://[<username>[:<password>]@]<host>/<database> - copied from sponge doc
            sqlURL.append(username).append(':').append(password).append('@')
                    .append(dbCfg.path)
                    .append(':')
                    .append(dbCfg.port)
                    .append('/')
                    .append(dbCfg.getDBName())
                    .append("?useSSL").append('=').append(dbCfg.useSSL);
        } else if (dbCfg.isSQLite()) {
            sqlURL.append(storagePath).append(File.separatorChar).append(pluginId).append(".db");
        } else {
            sqlURL.append(storagePath).append(File.separatorChar).append(pluginId);
        }
        this.jdbcUrl = sqlURL.toString();
    }

    public Connection getConnection() throws SQLException {
        if (sql == null) sql = Sponge.getServiceManager().provideUnchecked(SqlService.class);
        return sql.getDataSource(jdbcUrl).getConnection();
    }

    public Account getAccountIfPresent(Player player) {
        return cache.get(player.getUniqueId());
    }

    public boolean isOnline(Player player) {
        Account account = getAccountIfPresent(player);
        return account != null && account.isOnline();
    }

    public void createTable() {
        try {
            DatabaseMigration migration = new DatabaseMigration(manager);
            migration.migrateName();
            migration.createTable();
        } catch (SQLException sqlEx) {
            manager.console(ChatColor.RED + "Error creating database table");
        }
    }

    public Account deleteAccount(String identity) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("DELETE FROM " + USERS_TABLE + " WHERE `uuid`=? OR `username`=?");
            statement.setString(1, identity);
            statement.setString(2, identity);
            int affectedRows = statement.executeUpdate();
            //remove cache entry
            cache.values().stream()
                    .filter(account -> account.username().equals(identity) || account.uuid().equals(identity))
                    .map(Account::uuid)
                    .forEach(cache::remove);

            //min one account was found
            return affectedRows > 0 ? null : null;
        } catch (SQLException ex) {
            manager.console("Error deleting user account");
        } finally {
            closeQuietly(conn);
        }

        return null;
    }

    public Account deleteAccount(UUID uuid) {

        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("DELETE FROM " + USERS_TABLE + " WHERE UUID=?");

            byte[] mostBytes = Longs.toByteArray(uuid.getMostSignificantBits());
            byte[] leastBytes = Longs.toByteArray(uuid.getLeastSignificantBits());

            statement.setObject(1, Bytes.concat(mostBytes, leastBytes));

            int affectedRows = statement.executeUpdate();
            //removes the account from the cache
            cache.remove(uuid);

            //min one account was found
            //TODO
            return affectedRows > 0 ? null : null;
        } catch (SQLException sqlEx) {
            manager.console("Error deleting user account");
        } finally {
            closeQuietly(conn);
        }

        return null;
    }

    public Account loadAccount(Player player) {
        return loadAccount(player.getUniqueId());
    }

    public Account remove(Player player) {
        return cache.remove(player.getUniqueId());
    }

    public Account loadAccount(UUID uuid) {
        Account loadedAccount = null;
        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement prepareStatement = conn.prepareStatement("SELECT * FROM " + USERS_TABLE + " WHERE `uuid`=?");

            prepareStatement.setString(1, uuid.toString());

            ResultSet resultSet = prepareStatement.executeQuery();
            if (resultSet.next()) {
                loadedAccount = new Account(resultSet);
                cache.put(uuid, loadedAccount);
            }
        } catch (SQLException sqlEx) {
            manager.console("Error loading account");
        } finally {
            closeQuietly(conn);
        }

        return loadedAccount;
    }

    public Account loadAccount(String playerName) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("SELECT * FROM " + USERS_TABLE + " WHERE Username=?");
            statement.setString(1, playerName);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new Account(resultSet);
            }
        } catch (SQLException sqlEx) {
            manager.console("Error loading account");
        } finally {
            closeQuietly(conn);
        }

        return null;
    }

    public int getRegistrationsCount(String ip) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM " + USERS_TABLE + " WHERE IP=?");
            statement.setString(1, ip);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException sqlEx) {
            manager.console("Error loading count of registrations");
        } finally {
            closeQuietly(conn);
        }

        return -1;
    }

    public boolean createAccount(Account account, boolean shouldCache) {
        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement prepareStatement = conn.prepareStatement("INSERT INTO " + USERS_TABLE
                    + " (`uuid`, `username`, `password`, `ip`, `email`, `lastLogin`) VALUES (?,?,?,?,?,?)");

            prepareStatement.setString(1, account.uuid().toString());
            prepareStatement.setString(2, account.username());
            prepareStatement.setString(3, account.getPassword());

            prepareStatement.setString(4, account.ip());

            prepareStatement.setString(5, account.getEmail());
            prepareStatement.setTimestamp(6, account.getTimestamp());

            prepareStatement.execute();

            if (shouldCache) {
                cache.put(account.uuid(), account);
            }

            return true;
        } catch (SQLException sqlEx) {
            manager.console("Error registering account");
        } finally {
            closeQuietly(conn);
        }

        return false;
    }

    protected void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                //this closes automatically the statement and resultset
                conn.close();
            } catch (SQLException ex) {
                //ingore
            }
        }
    }

    public void flushLoginStatus(Account account, boolean online) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement prepareStatement = conn.prepareStatement("UPDATE " + USERS_TABLE
                    + " SET `online`=? WHERE `uuid`=?");

            prepareStatement.setInt(1, online ? 1 : 0);
            prepareStatement.setString(2, account.uuid().toString());

            prepareStatement.execute();
        } catch (SQLException ex) {
            manager.console("Error updating login status");
        } finally {
            closeQuietly(conn);
        }
    }

    public void close() {
        cache.clear();

        Connection conn = null;
        try {
            conn = getConnection();

            //set all player accounts existing in the database to unlogged
            conn.createStatement().execute("UPDATE " + USERS_TABLE + " SET `online`=0");
        } catch (SQLException ex) {
            if (manager.isDebug()) ex.printStackTrace();
            manager.console(ChatColor.RED + "Error updating user account !!");
        } finally {
            closeQuietly(conn);
        }
    }

    public boolean save(Account account) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("UPDATE " + USERS_TABLE
                    + " SET `username`=?, `password`=?, `ip`=?, `lastLogin`=?, `email`=? WHERE `uuid`=?");
            //username is now changeable by Mojang - so keep it up to date
            statement.setString(1, account.username());
            statement.setString(2, account.getPassword());
            statement.setString(3, account.ip());

            statement.setTimestamp(4, account.getTimestamp());
            statement.setString(5, account.getEmail());
            statement.setString(6, account.uuid().toString());

            statement.execute();
            cache.put(account.uuid(), account);
            return true;
        } catch (SQLException ex) {
            manager.console("Error updating user account");
            return false;
        } finally {
            closeQuietly(conn);
        }
    }
}
