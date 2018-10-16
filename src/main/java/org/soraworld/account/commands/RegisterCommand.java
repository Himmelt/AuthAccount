package org.soraworld.account.commands;

import com.google.common.collect.Lists;
import org.soraworld.account.AuthAccount;
import org.soraworld.account.tasks.RegisterTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Collection;
import java.util.List;

public class RegisterCommand implements CommandExecutor {

    private final AuthAccount plugin = AuthAccount.getInstance();

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(plugin.loader().getTextConfig().getPlayersOnlyActionMessage());
            return CommandResult.success();
        }

        if (plugin.loader().config().isPlayerPermissions()
                && !source.hasPermission(plugin.plugin().getId() + ".command.register")) {
            throw new CommandPermissionException();
        }

        //If the server is using TOTP, no password is required
        if (!args.hasAny("password")) {
            if ("totp".equals(plugin.loader().config().getHashAlgo())) {
                startTask(source, "");
            } else {
                source.sendMessage(plugin.loader().getTextConfig().getTotpNotEnabledMessage());
            }

            return CommandResult.success();
        }

        Collection<String> passwords = args.getAll("password");
        List<String> indexPasswords = Lists.newArrayList(passwords);
        String password = indexPasswords.get(0);
        if (password.equals(indexPasswords.get(1))) {
            if (password.length() >= plugin.loader().config().getMinPasswordLength()) {
                //Check if the first two passwords are equal to prevent typos
                startTask(source, password);
            } else {
                source.sendMessage(plugin.loader().getTextConfig().getTooShortPasswordMessage());
            }
        } else {
            source.sendMessage(plugin.loader().getTextConfig().getUnequalPasswordsMessage());
        }

        return CommandResult.success();
    }

    private void startTask(CommandSource source, String password) {
        Sponge.getScheduler().createTaskBuilder()
                //we are executing a SQL Query which is blocking
                .async()
                .execute(new RegisterTask((Player) source, password))
                .name("Register Query")
                .submit(plugin);
    }
}
