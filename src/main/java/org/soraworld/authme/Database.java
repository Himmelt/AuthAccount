package org.soraworld.authme;

import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.soraworld.authme.config.SQLConfiguration;
import org.soraworld.authme.config.SQLType;
import org.soraworld.authme.constant.Constant;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.sql.SqlService;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class Database {

    public static final String USERS_TABLE = Constant.TABLE_PREFIX + "accounts";

    private final Authme plugin = Authme.getInstance();
    //this cache is thread-safe
    private final Map<UUID, Account> cache = Maps.newConcurrentMap();

    private final String jdbcUrl;
    private final String username;
    private final String password;

    private SqlService sql;

    public Database() {
        SQLConfiguration sqlConfig = plugin.getCfgLoader().getConfig().getSqlConfiguration();

        if (sqlConfig.getType() == SQLType.MYSQL) {
            this.username = sqlConfig.getUsername();
            this.password = sqlConfig.getPassword();
        } else {
            //flat file drivers throw exception if you try to connect with a account
            this.username = "";
            this.password = "";
        }

        String storagePath = sqlConfig.getPath()
                .replace("%DIR%", plugin.getCfgLoader().getConfigDir().normalize().toString());

        StringBuilder urlBuilder = new StringBuilder("jdbc:")
                .append(sqlConfig.getType().name().toLowerCase()).append("://");
        switch (sqlConfig.getType()) {
            case SQLITE:
                urlBuilder.append(storagePath).append(File.separatorChar).append("database.db");
                break;
            case MYSQL:
                //jdbc:<engine>://[<username>[:<password>]@]<host>/<database> - copied from sponge doc
                urlBuilder.append(username).append(':').append(password).append('@')
                        .append(sqlConfig.getPath())
                        .append(':')
                        .append(sqlConfig.getPort())
                        .append('/')
                        .append(sqlConfig.getDatabase())
                        .append("?useSSL").append('=').append(sqlConfig.isUseSSL());
                break;
            case H2:
            default:
                urlBuilder.append(storagePath).append(File.separatorChar).append("database");
                break;
        }

        this.jdbcUrl = urlBuilder.toString();
    }

    public Connection getConnection() throws SQLException {
        if (sql == null) {
            //lazy binding
            sql = plugin.getGame().getServiceManager().provideUnchecked(SqlService.class);
        }

        return sql.getDataSource(jdbcUrl).getConnection();
    }

    public Account getAccountIfPresent(Player player) {
        return cache.get(player.getUniqueId());
    }

    public boolean isLoggedin(Player player) {
        Account account = getAccountIfPresent(player);
        return account != null && account.isLoggedIn();
    }

    public void createTable() {
        try {
            DatabaseMigration migration = new DatabaseMigration(plugin);
            migration.migrateName();
            migration.createTable();
        } catch (SQLException sqlEx) {
            plugin.getLogger().error("Error creating database table", sqlEx);
        }
    }

    public boolean deleteAccount(String username) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("DELETE FROM " + USERS_TABLE + " WHERE Username=?");
            statement.setString(1, username);

            int affectedRows = statement.executeUpdate();
            //remove cache entry
            cache.values().stream()
                    .filter(account -> account.username().equals(username))
                    .map(Account::uuid)
                    .forEach(cache::remove);

            //min one account was found
            return affectedRows > 0;
        } catch (SQLException ex) {
            plugin.getLogger().error("Error deleting user account", ex);
        } finally {
            closeQuietly(conn);
        }

        return false;
    }

    public boolean deleteAccount(UUID uuid) {
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
            return affectedRows > 0;
        } catch (SQLException sqlEx) {
            plugin.getLogger().error("Error deleting user account", sqlEx);
        } finally {
            closeQuietly(conn);
        }

        return false;
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
            PreparedStatement prepareStatement = conn.prepareStatement("SELECT * FROM " + USERS_TABLE
                    + " WHERE UUID=?");
            byte[] mostBytes = Longs.toByteArray(uuid.getMostSignificantBits());
            byte[] leastBytes = Longs.toByteArray(uuid.getLeastSignificantBits());

            prepareStatement.setObject(1, Bytes.concat(mostBytes, leastBytes));

            ResultSet resultSet = prepareStatement.executeQuery();
            if (resultSet.next()) {
                loadedAccount = new Account(resultSet);
                cache.put(uuid, loadedAccount);
            }
        } catch (SQLException sqlEx) {
            plugin.getLogger().error("Error loading account", sqlEx);
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
            plugin.getLogger().error("Error loading account", sqlEx);
        } finally {
            closeQuietly(conn);
        }

        return null;
    }

    public int getRegistrationsCount(byte[] ip) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM " + USERS_TABLE + " WHERE IP=?");
            statement.setBytes(1, ip);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException sqlEx) {
            plugin.getLogger().error("Error loading count of registrations", sqlEx);
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

            prepareStatement.setObject(1, account.uuid().toString());
            prepareStatement.setString(2, account.username());
            prepareStatement.setString(3, account.getPassword());

            prepareStatement.setObject(4, account.ip());

            prepareStatement.setString(5, account.getEmail());
            prepareStatement.setTimestamp(6, account.getTimestamp());

            prepareStatement.execute();

            if (shouldCache) {
                cache.put(account.uuid(), account);
            }

            return true;
        } catch (SQLException sqlEx) {
            plugin.getLogger().error("Error registering account", sqlEx);
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

    public void flushLoginStatus(Account account, boolean loggedIn) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement prepareStatement = conn.prepareStatement("UPDATE " + USERS_TABLE
                    + " SET LoggedIn=? WHERE UUID=?");

            prepareStatement.setInt(1, loggedIn ? 1 : 0);

            UUID uuid = account.uuid();
            byte[] mostBytes = Longs.toByteArray(uuid.getMostSignificantBits());
            byte[] leastBytes = Longs.toByteArray(uuid.getLeastSignificantBits());

            prepareStatement.setObject(2, Bytes.concat(mostBytes, leastBytes));

            prepareStatement.execute();
        } catch (SQLException ex) {
            plugin.getLogger().error("Error updating login status", ex);
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
            plugin.getLogger().error("Error updating user account", ex);
        } finally {
            closeQuietly(conn);
        }
    }

    public boolean save(Account account) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("UPDATE " + USERS_TABLE
                    + " SET Username=?, Password=?, IP=?, LastLogin=?, Email=? WHERE UUID=?");
            //username is now changeable by Mojang - so keep it up to date
            statement.setString(1, account.username());
            statement.setString(2, account.getPassword());
            statement.setObject(3, account.ip());

            statement.setTimestamp(4, account.getTimestamp());
            statement.setString(5, account.getEmail());

            UUID uuid = account.uuid();

            byte[] mostBytes = Longs.toByteArray(uuid.getMostSignificantBits());
            byte[] leastBytes = Longs.toByteArray(uuid.getLeastSignificantBits());

            statement.setObject(6, Bytes.concat(mostBytes, leastBytes));
            statement.execute();
            cache.put(uuid, account);
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().error("Error updating user account", ex);
            return false;
        } finally {
            closeQuietly(conn);
        }
    }
}
