package org.soraworld.account.command;

import org.soraworld.account.manager.AccountManager;
import org.soraworld.violet.command.Args;
import org.soraworld.violet.command.SpongeCommand;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class CommandLogin extends SpongeCommand {

    private final AccountManager manager;

    public CommandLogin(String name, String perm, AccountManager manager, String... aliases) {
        super(name, perm, true, manager, aliases);
        this.manager = manager;
    }

    public void execute(final Player player, Args args) {
        CommandAccount.login(args, manager, player);
    }

    public Text getUsage(CommandSource source) {
        return Text.of("/login <password>");
    }
}
