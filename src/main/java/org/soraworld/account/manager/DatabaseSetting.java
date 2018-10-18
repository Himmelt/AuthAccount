package org.soraworld.account.manager;

import org.soraworld.hocon.node.Serializable;
import org.soraworld.hocon.node.Setting;

@Serializable
public class DatabaseSetting {

    @Setting(comment = "comment.database.enable")
    public boolean enable = false;

    @Setting(comment = "SQL server type. You can choose between h2, SQLite and MySQL")
    public String type = "h2";

    @Setting(comment = "Path where the database is located. This can be a file path (h2/SQLite) or an IP/Domain(MySQL)")
    private String path = "%DIR%";

    @Setting(comment = "Port for example MySQL connections")
    private int port = 3306;

    @Setting(comment = "Database name")
    private String database = "sponge";

    @Setting
    String table = "accounts";

    @Setting(comment = "Username to login the database system")
    private String username = "";

    @Setting(comment = "Password in order to login")
    private String password = "";

    @Setting(comment = "It's strongly recommended to enable SSL and setup a SSL certificate if the MySQL server isn't " +
            "running on the same machine")
    private boolean useSSL = false;

    public boolean isMySQL() {
        return "MySQL".equalsIgnoreCase(type);
    }

    public boolean isH2() {
        return "H2".equalsIgnoreCase(type);
    }

    public boolean isSQLite() {
        return "SQLite".equalsIgnoreCase(type);
    }
}
