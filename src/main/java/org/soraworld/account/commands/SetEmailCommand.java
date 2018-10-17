package org.soraworld.account.commands;

import org.soraworld.account.data.Account;
import org.soraworld.account.AuthAccount;
import org.soraworld.account.tasks.SaveTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public class SetEmailCommand implements CommandExecutor {

    private static final String EMAIL_REGEX = "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+";

    private final AuthAccount plugin = AuthAccount.getInstance();

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {


    }
}
