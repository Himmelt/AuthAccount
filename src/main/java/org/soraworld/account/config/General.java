package org.soraworld.account.config;

import org.soraworld.hocon.node.Serializable;
import org.soraworld.hocon.node.Setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

@Serializable
public class General {
    @Setting(comment = "comment.general.nameRegex")
    public Pattern nameRegex = Pattern.compile("[A-Za-z0-9_-]{3,15}");
    @Setting(comment = "comment.general.pswdRegex")
    public Pattern pswdRegex = Pattern.compile("[!-~]{4,16}");
    @Setting(comment = "Should the plugin login users automatically if it's the same account from the same IP")
    public boolean ipAutoLogin = false;
    @Setting(comment = "Should only the specified commands be protected from unauthorized access")
    public boolean commandOnlyProtection;
    @Setting(comment = "The user should use a strong password")
    public int minPasswordLength = 4;
    @Setting(comment = "Number of seconds a player has time to login or will be kicked.-1 deactivates this features")
    public int timeoutLogin = 60;
    @Setting(comment = "Should this plugin check for player permissions")
    public boolean playerPermissions;
    @Setting(comment = "Teleport the player to a safe location based on the last login coordinates")
    public boolean safeLocation;
    @Setting(comment = "Should the plugin save the login status to the database")
    public boolean updateLoginStatus;
    @Setting(comment = "How many login attempts are allowed until everything is blocked")
    public int maxAttempts = 3;
    @Setting(comment = "How seconds the user should wait after the user tried to make too many attempts")
    public int waitTime = 300;
    @Setting(comment = "How many accounts are allowed per ip-addres. Use 0 to disable it")
    public int maxIpReg = 0;
    @Setting(comment = "Interval where the please login will be printed to the user")
    public int messageInterval = 2;
    @Setting(comment = "comment.banNames")
    public ArrayList<String> banNames = new ArrayList<>(Arrays.asList(
            "___",
            "---",
            "123",
            "1234",
            "12345",
            "op",
            "server",
            "admin",
            "administrator",
            "mc",
            "minecraft",
            "notch"));
    @Setting(comment = "comment.allowCommands")
    public HashSet<String> allowCommands = new HashSet<>();
}
