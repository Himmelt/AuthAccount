package org.soraworld.account.config;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.hocon.node.Serializable;
import org.soraworld.hocon.node.Setting;
import org.soraworld.violet.util.ChatColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.sql.SqlService;

import java.io.File;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Serializable
public class Database {
    @Setting(comment = "comment.database.enable")
    private boolean enable = false;
    @Setting(comment = "comment.database.type")
    private String type = "H2";
    @Setting(comment = "comment.database.host")
    private String host = "%path%";
    @Setting(comment = "comment.database.port")
    private int port = 3306;
    @Setting(comment = "comment.database.name")
    private String name = "sponge";
    @Setting(comment = "comment.database.table")
    private String table = "accounts";
    @Setting(comment = "comment.database.username")
    private String username = "";
    @Setting(comment = "comment.database.password")
    private String password = "";
    @Setting(comment = "comment.database.useSSL")
    private boolean useSSL = false;

    private SqlService sql;
    private String jdbcURL;
    private final String storage;
    private final AccountManager manager;
    private static final ConcurrentHashMap<UUID, Account> cache = new ConcurrentHashMap<>();

    public Database(AccountManager manager, Path root) {
        this.manager = manager;
        this.storage = host.replace("%path%", root.normalize().toString());
    }

    private String getJdbcURL() {
        if (jdbcURL == null) {
            StringBuilder builder = new StringBuilder("jdbc:").append(type.toLowerCase()).append("://");
            if ("MySQL".equalsIgnoreCase(type)) {
                //jdbc:<engine>://[<username>[:<password>]@]<host>/<database>
                builder.append(username).append(':').append(password).append('@')
                        .append(host).append(':').append(port).append('/')
                        .append(name).append("?useSSL").append('=').append(useSSL);
            } else if ("SQLite".equalsIgnoreCase(type)) {
                builder.append(storage).append(File.separatorChar).append(manager.getPlugin().getId()).append(".db");
            } else {
                builder.append(storage).append(File.separatorChar).append(manager.getPlugin().getId());
            }
            jdbcURL = builder.toString();
        }
        return jdbcURL;
    }

    public Connection getConnection() throws SQLException {
        if (sql == null) sql = Sponge.getServiceManager().provideUnchecked(SqlService.class);
        return sql.getDataSource(getJdbcURL()).getConnection();
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
            createTable2();
        } catch (SQLException sqlEx) {
            manager.console(ChatColor.RED + "Error creating database table");
        }
    }

    public void createTable2() throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();

            boolean tableExists = false;
            try {
                //check if the table already exists
                Statement statement = conn.createStatement();
                statement.execute("SELECT 1 FROM " + table);
                statement.close();

                tableExists = true;
            } catch (SQLException sqlEx) {
                manager.console("Table " + table + " doesn't exist,will create it!");
            }

            if (!tableExists) {
                if ("SQLite".equalsIgnoreCase(type)) {
                    Statement statement = conn.createStatement();
                    statement.execute("CREATE TABLE " + table + " ( "
                            + "`userid` INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "`uuid` char(36) NOT NULL DEFAULT '' , "
                            + "`username` char(16) NOT NULL DEFAULT '' , "
                            + "`password` varchar(64) NOT NULL DEFAULT '' , "
                            + "`ip` varchar(32) NOT NULL DEFAULT '' , "
                            + "`lastlogin` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  , "
                            + "`email` VARCHAR(64) DEFAULT NULL , "
                            + "`online` BOOLEAN DEFAULT 0, "
                            + "UNIQUE (`uuid`) "
                            + ')');
                    statement.close();
                } else {
                    Statement statement = conn.createStatement();
                    statement.execute("CREATE TABLE " + table + " ( "
                            + "`userid` INT UNSIGNED NOT NULL AUTO_INCREMENT , "
                            + "`uuid` char(36) NOT NULL DEFAULT '' COMMENT 'UUID' , "
                            + "`username` char(16) NOT NULL DEFAULT '' COMMENT 'Username' , "
                            + "`password` varchar(64) NOT NULL DEFAULT '' , "
                            + "`ip` varchar(32) NOT NULL DEFAULT '' , "
                            + "`lastlogin` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP , "
                            + "`email` VARCHAR(64) DEFAULT NULL , "
                            + "`online` BOOLEAN DEFAULT 0, "
                            + "PRIMARY KEY (`userid`) , UNIQUE (`uuid`) "
                            + ')');
                    statement.close();
                }
            }
        } finally {
            closeQuietly(conn);
        }
    }

    public Account deleteAccount(String identity) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("DELETE FROM " + table + " WHERE `uuid`=? OR `username`=?");
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

            PreparedStatement statement = conn.prepareStatement("DELETE FROM " + table + " WHERE UUID=?");

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

    public Account remove(Player player) {
        return cache.remove(player.getUniqueId());
    }

    public Account loadAccount(UUID uuid) {
        Account loadedAccount = null;
        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement prepareStatement = conn.prepareStatement("SELECT * FROM " + table + " WHERE `uuid`=?");

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

            PreparedStatement statement = conn.prepareStatement("SELECT * FROM " + table + " WHERE Username=?");
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

            PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE IP=?");
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
            PreparedStatement prepareStatement = conn.prepareStatement("INSERT INTO " + table
                    + " (`uuid`, `username`, `password`, `ip`, `email`, `lastLogin`) VALUES (?,?,?,?,?,?)");

            prepareStatement.setString(1, account.uuid().toString());
            prepareStatement.setString(2, account.username());
            prepareStatement.setString(3, account.password());

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

            PreparedStatement prepareStatement = conn.prepareStatement("UPDATE " + table
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
            conn.createStatement().execute("UPDATE " + table + " SET `online`=0");
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

            PreparedStatement statement = conn.prepareStatement("UPDATE " + table
                    + " SET `username`=?, `password`=?, `ip`=?, `lastLogin`=?, `email`=? WHERE `uuid`=?");
            //username is now changeable by Mojang - so keep it up to date
            statement.setString(1, account.username());
            statement.setString(2, account.password());
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
