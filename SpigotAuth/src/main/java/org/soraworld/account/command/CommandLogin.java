package org.soraworld.account.command;

import org.bukkit.entity.Player;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.violet.command.Args;
import org.soraworld.violet.command.SpigotCommand;

public class CommandLogin extends SpigotCommand {

    private final AccountManager manager;

    public CommandLogin(String name, String perm, AccountManager manager, String... aliases) {
        super(name, perm, true, manager, aliases);
        this.manager = manager;
    }

    public void execute(final Player player, Args args) {
        CommandAccount.login(args, manager, player);
    }

    public String getUsage() {
        return "/login <password>";
    }
}
