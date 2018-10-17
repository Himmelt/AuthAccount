package org.soraworld.account.commands;

import org.soraworld.account.data.Account;
import org.soraworld.account.AuthAccount;
import org.soraworld.account.tasks.LoginTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public class LoginCommand implements CommandExecutor {

    private final AuthAccount plugin = AuthAccount.getInstance();

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(plugin.loader().getTextConfig().getPlayersOnlyActionMessage());
            return CommandResult.empty();
        }

        if (plugin.loader().config().isPlayerPermissions()
                && !source.hasPermission(plugin.plugin().getId() + ".command.login")) {
            throw new CommandPermissionException();
        }


        return CommandResult.success();
    }
}
