package org.soraworld.account.data;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.soraworld.account.constant.Constant;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.manager.DatabaseSetting;
import org.soraworld.violet.util.ChatColor;
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
    private final ConcurrentHashMap<String, Account> cache = new ConcurrentHashMap<>();

    private final String jdbcUrl;
    private final String username;
    private final String password;

    private SqlService sql;

    private final AccountManager manager;
    private final Path path;

    public Database(AccountManager manager, Path path) {
        this.manager = manager;
        this.path = path;
        DatabaseSetting database = manager.databaseSetting;

        if (database.isMySQL()) {
            this.username = database.getUsername();
            this.password = database.getPassword();
        } else {
            this.username = "";
            this.password = "";
        }

        String storagePath = database.getPath()
                .replace("%DIR%", manager.path.normalize());

        StringBuilder urlBuilder = new StringBuilder("jdbc:")
                .append(database.getType().name().toLowerCase()).append("://");
        switch (database.getType()) {
            case SQLITE:
                urlBuilder.append(storagePath).append(File.separatorChar).append(Constant.MODID + ".db");
                break;
            case MYSQL:
                //jdbc:<engine>://[<username>[:<password>]@]<host>/<database> - copied from sponge doc
                urlBuilder.append(username).append(':').append(password).append('@')
                        .append(database.getPath())
                        .append(':')
                        .append(database.getPort())
                        .append('/')
                        .append(database.getDatabase())
                        .append("?useSSL").append('=').append(database.isUseSSL());
                break;
            case H2:
            default:
                urlBuilder.append(storagePath).append(File.separatorChar).append(Constant.MODID);
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
        return cache.get(player.getUniqueId().toString());
    }

    public boolean isOnline(Player player) {
        Account account = getAccountIfPresent(player);
        return account != null && account.isOnline();
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

    public boolean deleteAccount(String identity) {
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
        return cache.remove(player.getUniqueId().toString());
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
                cache.put(uuid.toString(), loadedAccount);
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

            prepareStatement.setString(1, account.uuid());
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

    public void flushLoginStatus(Account account, boolean online) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement prepareStatement = conn.prepareStatement("UPDATE " + USERS_TABLE
                    + " SET `online`=? WHERE `uuid`=?");

            prepareStatement.setInt(1, online ? 1 : 0);
            prepareStatement.setString(2, account.uuid());

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
            statement.setString(6, account.uuid());

            statement.execute();
            cache.put(account.uuid(), account);
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().error("Error updating user account", ex);
            return false;
        } finally {
            closeQuietly(conn);
        }
    }
}
