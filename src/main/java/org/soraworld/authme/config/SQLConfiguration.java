package org.soraworld.authme.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class SQLConfiguration {

    @Setting(comment = "SQL server type. You can choose between h2, SQLite and MySQL")
    private SQLType type = SQLType.H2;

    @Setting(comment = "Path where the database is located. This can be a file path (h2/SQLite) or an IP/Domain(MySQL)")
    private String path = "%DIR%";

    @Setting(comment = "Port for example MySQL connections")
    private int port = 3306;

    @Setting(comment = "Database name")
    private String database = "sponge";

    @Setting(comment = "Username to login the database system")
    private String username = "";

    @Setting(comment = "Password in order to login")
    private String password = "";

    @Setting(comment = "It's strongly recommended to enable SSL and setup a SSL certificate if the MySQL server isn't " +
            "running on the same machine")
    private boolean useSSL = false;

    public SQLType getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isUseSSL() {
        return useSSL;
    }
}
