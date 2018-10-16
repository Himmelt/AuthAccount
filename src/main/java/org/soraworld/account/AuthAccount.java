package org.soraworld.account;

import org.soraworld.account.command.CommandAccount;
import org.soraworld.account.command.CommandLogin;
import org.soraworld.account.listener.ConnectionListener;
import org.soraworld.account.listener.PreventListener;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.violet.command.SpongeBaseSubs;
import org.soraworld.violet.command.SpongeCommand;
import org.soraworld.violet.manager.SpongeManager;
import org.soraworld.violet.plugin.SpongePlugin;
import org.spongepowered.api.plugin.Plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Plugin(
        id = AuthAccount.PLUGIN_ID,
        name = AuthAccount.PLUGIN_NAME,
        version = AuthAccount.PLUGIN_VERSION
)
public class AuthAccount extends SpongePlugin {

    public static final String PLUGIN_ID = "authaccount";
    public static final String PLUGIN_NAME = "AuthAccount";
    public static final String PLUGIN_VERSION = "1.0.0";

    protected SpongeManager registerManager(Path path) {
        return new AccountManager(this, path);
    }

    protected List<Object> registerListeners() {
        List<Object> list = new ArrayList<>();
        list.add(new ConnectionListener(manager));
        list.add(new PreventListener());
        return list;
    }

    protected void registerCommands() {
        SpongeCommand command = new SpongeCommand(getId(), null, false, manager, "auth", "account", "acc");
        command.extractSub(SpongeBaseSubs.class);
        command.extractSub(CommandAccount.class);
        register(this, command);
        register(this, new CommandLogin("login", null, false, manager, "log", "l"));
    }

    public void beforeDisable() {
        ((AccountManager) manager).closeDatabase();
        ((AccountManager) manager).unProtectAll();
    }
}
