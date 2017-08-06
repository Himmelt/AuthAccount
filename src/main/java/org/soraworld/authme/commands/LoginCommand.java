package org.soraworld.authme.commands;

import org.soraworld.authme.Account;
import org.soraworld.authme.Authme;
import org.soraworld.authme.tasks.LoginTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public class LoginCommand implements CommandExecutor {

    private final Authme plugin = Authme.getInstance();

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(plugin.getCfgLoader().getTextConfig().getPlayersOnlyActionMessage());
            return CommandResult.empty();
        }

        if (plugin.getCfgLoader().getConfig().isPlayerPermissions()
                && !source.hasPermission(plugin.getContainer().getId() + ".command.login")) {
            throw new CommandPermissionException();
        }

        Account account = plugin.getDatabase().getAccountIfPresent((Player) source);
        if (account != null && account.isOnline()) {
            source.sendMessage(plugin.getCfgLoader().getTextConfig().getAlreadyLoggedInMessage());
        }

        //the arg isn't optional. We can be sure there is value
        String password = args.<String>getOne("password").get();

        Sponge.getScheduler().createTaskBuilder()
                //we are executing a SQL Query which is blocking
                .async()
                .execute(new LoginTask((Player) source, password))
                .name("Login Query")
                .submit(plugin);

        return CommandResult.success();
    }
}
