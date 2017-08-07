package org.soraworld.authme.config;

import com.google.common.collect.Lists;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.List;

public class Config {

    @Setting(comment = "Database configuration")
    private SQLConfiguration sqlConfiguration = new SQLConfiguration();

    @Setting(comment = "Email configuration for password recovery")
    private EmailConfiguration emailConfiguration = new EmailConfiguration();

    @Setting(comment = "Algorithms for hashing user passwords. You can also choose totp")
    private String hashAlgo = "bcrypt";

    @Setting(comment = "Should the plugin login users automatically if it's the same account from the same IP")
    private boolean ipAutoLogin = false;

    @Setting(comment = "Should only the specified commands be protected from unauthorized access")
    private boolean commandOnlyProtection;

    @Setting(comment = "The user should use a strong password")
    private int minPasswordLength = 4;

    @Setting(comment = "Number of seconds a player has time to login or will be kicked.-1 deactivates this features")
    private int timeoutLogin = 60;

    @Setting(comment = "Should this plugin check for player permissions")
    private boolean playerPermissions;

    @Setting(comment = "Teleport the player to a safe location based on the last login coordinates")
    private boolean safeLocation;

    @Setting(comment = "Should the plugin save the login status to the database")
    private boolean updateLoginStatus;

    @Setting(comment = "Do you allow your users to skip authentication with the bypass permission")
    private boolean bypassPermission;

    @Setting(comment = "How many login attempts are allowed until everything is blocked")
    private int maxAttempts = 3;

    @Setting(comment = "How seconds the user should wait after the user tried to make too many attempts")
    private int waitTime = 300;

    @Setting(comment = "Custom command that should run after the user tried to make too many attempts")
    private String lockCommand = "";

    @Setting(comment = "How many accounts are allowed per ip-addres. Use 0 to disable it")
    private int maxIpReg = 0;

    @Setting(comment = "Interval where the please login will be printed to the user")
    private int messageInterval = 2;

    @Setting
    private SpawnTeleportConfig teleportConfig = new SpawnTeleportConfig();

    @Setting(comment = "If command only protection is enabled, these commands are protected. If the list is empty"
            + " all commands are protected")
    private List<String> protectedCommands = Lists.newArrayList("op", "pex");

    public EmailConfiguration getEmailConfiguration() {
        return emailConfiguration;
    }

    public SQLConfiguration getSqlConfiguration() {
        return sqlConfiguration;
    }

    public String getHashAlgo() {
        return hashAlgo;
    }

    public boolean isIpAutoLogin() {
        return ipAutoLogin;
    }

    public int getMinPasswordLength() {
        return minPasswordLength;
    }

    public boolean isCommandOnlyProtection() {
        return commandOnlyProtection;
    }

    public int getTimeoutLogin() {
        return timeoutLogin;
    }

    public boolean isUpdateLoginStatus() {
        return updateLoginStatus;
    }

    public boolean isPlayerPermissions() {
        return playerPermissions;
    }

    public boolean bypass() {
        return bypassPermission;
    }

    public SpawnTeleportConfig getTeleportConfig() {
        return teleportConfig;
    }

    public List<String> getProtectedCommands() {
        return protectedCommands;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public int getMaxIpReg() {
        return maxIpReg;
    }

    public boolean isSafeLocation() {
        return safeLocation;
    }

    public String getLockCommand() {
        return lockCommand;
    }

    public int getMessageInterval() {
        return messageInterval;
    }
}
