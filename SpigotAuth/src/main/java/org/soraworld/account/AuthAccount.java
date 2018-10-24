package org.soraworld.account;

import org.bukkit.event.Listener;
import org.soraworld.account.command.CommandAccount;
import org.soraworld.account.command.CommandLogin;
import org.soraworld.account.listener.EventListener;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.violet.command.SpigotBaseSubs;
import org.soraworld.violet.command.SpigotCommand;
import org.soraworld.violet.manager.SpigotManager;
import org.soraworld.violet.plugin.SpigotPlugin;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class AuthAccount extends SpigotPlugin {

    private final HashSet<String> cmdNames = new HashSet<>();

    protected SpigotManager registerManager(Path path) {
        return new AccountManager(this, path);
    }

    protected List<Listener> registerListeners() {
        return Collections.singletonList(new EventListener((AccountManager) manager));
    }

    protected void registerCommands() {
        SpigotCommand command = new SpigotCommand(getId(), null, false, manager, "auth", "account", "acc");
        command.extractSub(SpigotBaseSubs.class);
        command.extractSub(CommandAccount.class);
        register(this, command);
        register(this, new CommandLogin("login", null, (AccountManager) manager, "log", "l"));

        cmdNames.clear();
        for (SpigotCommand cmd : commands) {
            cmdNames.add(cmd.getName());
            cmdNames.add(getId() + ":" + cmd.getName());
            cmd.getAliases().forEach(alia -> {
                cmdNames.add(alia);
                cmdNames.add(getId() + ":" + alia);
            });
        }
    }

    public void afterEnable() {
        ((AccountManager) manager).protectAll();
    }

    public void beforeDisable() {
        ((AccountManager) manager).closeDatabase();
        ((AccountManager) manager).unProtectAll();
    }

    public Collection<String> getCmdNames() {
        return cmdNames;
    }
}
