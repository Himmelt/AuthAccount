package org.soraworld.authme.commands;

import org.soraworld.authme.Authme;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

public class ReloadCommand implements CommandExecutor {

    private final Authme plugin = Authme.getInstance();

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        plugin.onReload();

        source.sendMessage(plugin.loader().getTextConfig().getReloadMessage());
        return CommandResult.success();
    }
}
