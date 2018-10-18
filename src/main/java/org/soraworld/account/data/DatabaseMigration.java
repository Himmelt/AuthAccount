package org.soraworld.account.data;

import org.soraworld.account.manager.AccountManager;
import org.soraworld.violet.util.ChatColor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseMigration {

    private static final String OLD_TABLE_NAME = "users";

    private final AccountManager manager;

    public DatabaseMigration(AccountManager manager) {
        this.manager = manager;
    }

    public void createTable() throws SQLException {
        Connection conn = null;
        try {
            conn = manager.getDatabase().getConnection();

            boolean tableExists = false;
            try {
                //check if the table already exists
                Statement statement = conn.createStatement();
                statement.execute("SELECT 1 FROM " + Database.USERS_TABLE);
                statement.close();

                tableExists = true;
            } catch (SQLException sqlEx) {
                manager.console("Table " + Database.USERS_TABLE + " doesn't exist,will create it!");
            }

            if (!tableExists) {
                if (manager.databaseSetting.isSQLite()) {
                    Statement statement = conn.createStatement();
                    statement.execute("CREATE TABLE " + Database.USERS_TABLE + " ( "
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
                    statement.execute("CREATE TABLE " + Database.USERS_TABLE + " ( "
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
            manager.getDatabase().closeQuietly(conn);
        }
    }

    public void migrateName() {
        Connection conn = null;
        try {
            conn = manager.getDatabase().getConnection();

            boolean tableExists = false;
            try {
                //check if the table already exists
                Statement statement = conn.createStatement();
                statement.execute("SELECT 1 FROM " + OLD_TABLE_NAME);
                statement.close();

                tableExists = true;
            } catch (SQLException sqlEx) {
                //Old Table doesn't exist
            }

            if (tableExists) {
                Statement statement = conn.createStatement();
                statement.execute("SELECT 1 FROM " + OLD_TABLE_NAME);

                //if no error happens the table exists
                statement.execute("INSERT INTO " + Database.USERS_TABLE + " SELECT *, 0 FROM " + OLD_TABLE_NAME);
                statement.execute("DROP TABLE " + OLD_TABLE_NAME);
            }
        } catch (SQLException ex) {
            manager.console(ChatColor.RED + "Error migrating database");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    //ignore
                }
            }
        }
    }
}
