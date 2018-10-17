package org.soraworld.account.command;

import org.soraworld.account.data.Account;
import org.soraworld.account.manager.AccountManager;
import org.soraworld.account.tasks.LoginTask;
import org.soraworld.violet.command.Args;
import org.soraworld.violet.command.SpongeCommand;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

public class CommandLogin extends SpongeCommand {

    private final AccountManager manager;

    public CommandLogin(String name, String perm, AccountManager manager, String... aliases) {
        super(name, perm, true, manager, aliases);
        this.manager = manager;
    }

    public void execute(Player player, Args args) {
        Account account = manager.getDatabase().getAccountIfPresent(player);
        if (account != null && account.isOnline()) {
            manager.sendKey(player, "AlreadyLoggedInMessage");
            return;
        }

        //the arg isn't optional. We can be sure there is value

        Task.builder()
                //we are executing a SQL Query which is blocking
                .async()
                .execute(new LoginTask(manager, player, args.first()))
                .name("Login Query")
                .submit(manager.getPlugin());

    }

    public Text getUsage(CommandSource source) {
        return Text.of("/login <password>");
    }
}
