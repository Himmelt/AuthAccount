package org.soraworld.authme;

import org.soraworld.authme.config.SQLType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseMigration {

    private static final String OLD_TABLE_NAME = "users";

    private final Authme plugin;

    public DatabaseMigration(Authme plugin) {
        this.plugin = plugin;
    }

    public void createTable() throws SQLException {
        Connection conn = null;
        try {
            conn = plugin.getDatabase().getConnection();

            boolean tableExists = false;
            try {
                //check if the table already exists
                Statement statement = conn.createStatement();
                statement.execute("SELECT 1 FROM " + Database.USERS_TABLE);
                statement.close();

                tableExists = true;
            } catch (SQLException sqlEx) {
                plugin.getLogger().debug("Table doesn't exist", sqlEx);
            }

            if (!tableExists) {
                if (plugin.getCfgLoader().getConfig().getSqlConfiguration().getType() == SQLType.SQLITE) {
                    Statement statement = conn.createStatement();
                    statement.execute("CREATE TABLE " + Database.USERS_TABLE + " ( "
                            + "`UserID` INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "`UUID` BINARY(16) NOT NULL , "
                            + "`Username` VARCHAR , "
                            + "`Password` VARCHAR(64) NOT NULL , "
                            + "`IP` BINARY(32) NOT NULL , "
                            + "`LastLogin` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP , "
                            + "`Email` VARCHAR(64) DEFAULT NULL , "
                            + "`LoggedIn` BOOLEAN DEFAULT 0, "
                            + "UNIQUE (`UUID`) "
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
            plugin.getDatabase().closeQuietly(conn);
        }
    }

    public void migrateName() {
        Connection conn = null;
        try {
            conn = plugin.getDatabase().getConnection();

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
            plugin.getLogger().error("Error migrating database", ex);
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
