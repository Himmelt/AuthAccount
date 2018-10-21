package org.soraworld.account.config;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.hocon.node.Serializable;
import org.soraworld.hocon.node.Setting;
import org.soraworld.violet.util.ChatColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;

import java.io.File;
import java.sql.*;
import java.util.UUID;

@Serializable
public class Database {
    @Setting(comment = "comment.database.enable")
    public boolean enable = false;
    @Setting(comment = "comment.database.type")
    public String type = "H2";
    @Setting(comment = "comment.database.host")
    public String host = "%path%";
    @Setting(comment = "comment.database.port")
    public int port = 3306;
    @Setting(comment = "comment.database.name")
    public String name = "sponge";
    @Setting(comment = "comment.database.fallBack")
    public boolean fallBack = true;
    @Setting(comment = "comment.database.accountTable")
    private String accountTable = "accounts";
    @Setting(comment = "comment.database.statusTable")
    private String statusTable = "statuses";
    @Setting(comment = "comment.database.username")
    public String username = "";
    @Setting(comment = "comment.database.password")
    public String password = "";
    @Setting(comment = "comment.database.useSSL")
    public boolean useSSL = false;

    private SqlService sql;
    private String jdbcURL;
    private final String storage;
    private final AccountManager manager;

    public Database(AccountManager manager) {
        this.manager = manager;
        this.storage = host.replace("%path%", manager.getPath().normalize().toString());
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
                statement.execute("SELECT 1 FROM " + accountTable);
                statement.close();

                tableExists = true;
            } catch (SQLException sqlEx) {
                manager.console("Table " + accountTable + " doesn't exist,will create it!");
            }

            if (!tableExists) {
                if ("SQLite".equalsIgnoreCase(type)) {
                    Statement statement = conn.createStatement();
                    statement.execute("CREATE TABLE " + accountTable + " ( "
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
                    statement.execute("CREATE TABLE " + accountTable + " ( "
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
        // TODO return Account
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("DELETE FROM " + accountTable + " WHERE `uuid`=? OR `username`=?");
            statement.setString(1, identity);
            statement.setString(2, identity);
            int affectedRows = statement.executeUpdate();
            //remove cache entry

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
        // TODO return Account
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("DELETE FROM " + accountTable + " WHERE UUID=?");

            byte[] mostBytes = Longs.toByteArray(uuid.getMostSignificantBits());
            byte[] leastBytes = Longs.toByteArray(uuid.getLeastSignificantBits());

            statement.setObject(1, Bytes.concat(mostBytes, leastBytes));

            int affectedRows = statement.executeUpdate();
            //removes the account from the cache
            //min one account was found
            return affectedRows > 0 ? null : null;
        } catch (SQLException sqlEx) {
            manager.console("Error deleting user account");
        } finally {
            closeQuietly(conn);
        }

        return null;
    }

    public Account queryAccount(UUID uuid) {
        Account account = null;
        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement statement = conn.prepareStatement("SELECT * FROM " + accountTable + " WHERE `uuid`=?");

            statement.setString(1, uuid.toString());

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                account = new Account(result);
                //cache.put(uuid, account);
            }
        } catch (SQLException e) {
            manager.console("Error loading account");
        } finally {
            closeQuietly(conn);
        }

        return account;
    }

    public Account queryAccount(String playerName) {
        Connection conn = null;
        try {
            conn = getConnection();

            PreparedStatement statement = conn.prepareStatement("SELECT * FROM " + accountTable + " WHERE Username=?");
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

            PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM " + accountTable + " WHERE IP=?");
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
            PreparedStatement prepareStatement = conn.prepareStatement("INSERT INTO " + accountTable
                    + " (`uuid`, `username`, `password`, `ip`, `email`, `lastLogin`) VALUES (?,?,?,?,?,?)");

            prepareStatement.setString(1, account.uuid().toString());
            prepareStatement.setString(2, account.username());
            prepareStatement.setString(3, account.password());

            prepareStatement.setString(4, account.ip());

            prepareStatement.setString(5, account.getEmail());
            prepareStatement.setTimestamp(6, account.getTimestamp());

            prepareStatement.execute();

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

            PreparedStatement prepareStatement = conn.prepareStatement("UPDATE " + accountTable
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
        Connection conn = null;
        try {
            conn = getConnection();
            //set all player accounts existing in the database to unlogged
            conn.createStatement().execute("UPDATE " + accountTable + " SET `online`=0");
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

            PreparedStatement statement = conn.prepareStatement("UPDATE " + accountTable
                    + " SET `username`=?, `password`=?, `ip`=?, `lastLogin`=?, `email`=? WHERE `uuid`=?");
            //username is now changeable by Mojang - so keep it up to date
            statement.setString(1, account.username());
            statement.setString(2, account.password());
            statement.setString(3, account.ip());

            statement.setTimestamp(4, account.getTimestamp());
            statement.setString(5, account.getEmail());
            statement.setString(6, account.uuid().toString());

            statement.execute();
            return true;
        } catch (SQLException e) {
            manager.console("Error updating user account");
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    public void setOffline(UUID uuid) {

    }
}
