package org.soraworld.account.manager;

import com.google.common.collect.Lists;
import org.soraworld.hocon.node.Serializable;
import org.soraworld.hocon.node.Setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Serializable
class GeneralSetting {

    @Setting(comment = "Algorithms for hashing user passwords. You can also choose totp")
    public String hashAlgo = "bcrypt";

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

    @Setting(comment = "Do you allow your users to skip authentication with the bypass permission")
    public boolean bypassPermission;

    @Setting(comment = "How many login attempts are allowed until everything is blocked")
    public int maxAttempts = 3;

    @Setting(comment = "How seconds the user should wait after the user tried to make too many attempts")
    public int waitTime = 300;

    @Setting(comment = "Custom command that should run after the user tried to make too many attempts")
    public String lockCommand = "";

    @Setting(comment = "How many accounts are allowed per ip-addres. Use 0 to disable it")
    public int maxIpReg = 0;

    @Setting(comment = "Interval where the please login will be printed to the user")
    public int messageInterval = 2;

    @Setting(comment = "comment.keepNames")
    public ArrayList<String> keepNames = new ArrayList<>(Arrays.asList("op", "server", "admin", "administrator", "notch"));

    @Setting(comment = "If command only protection is enabled, these commands are protected. If the list is empty"
            + " all commands are protected")
    private List<String> protectedCommands = Lists.newArrayList("op", "pex");
}
