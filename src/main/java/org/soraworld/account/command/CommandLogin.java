package org.soraworld.account.command;

import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.tasks.LoginTask;
import org.soraworld.violet.command.Args;
import org.soraworld.violet.command.SpongeCommand;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import static org.soraworld.account.manager.AccountManager.getAccount;

public class CommandLogin extends SpongeCommand {

    private final AccountManager manager;

    public CommandLogin(String name, String perm, AccountManager manager, String... aliases) {
        super(name, perm, true, manager, aliases);
        this.manager = manager;
    }

    public void execute(final Player player, Args args) {
        if (getAccount(player.getUniqueId()).offline()) {
            if (args.notEmpty()) {
                Task.builder().async().name("LoginQuery")
                        .execute(new LoginTask(manager, player, args.first()))
                        .submit(manager.getPlugin());
            } else manager.sendKey(player, "emptyArgs");
        } else manager.sendKey(player, "alreadyLoggedIn");
    }

    public Text getUsage(CommandSource source) {
        return Text.of("/login <password>");
    }
}
