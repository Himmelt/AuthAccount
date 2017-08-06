package org.soraworld.authme.commands;

import com.google.common.collect.Lists;
import org.soraworld.authme.Authme;
import org.soraworld.authme.tasks.RegisterTask;
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

    private final Authme plugin = Authme.getInstance();

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(plugin.getCfgLoader().getTextConfig().getPlayersOnlyActionMessage());
            return CommandResult.success();
        }

        if (plugin.getCfgLoader().getConfig().isPlayerPermissions()
                && !source.hasPermission(plugin.getContainer().getId() + ".command.register")) {
            throw new CommandPermissionException();
        }

        //If the server is using TOTP, no password is required
        if (!args.hasAny("password")) {
            if ("totp".equals(plugin.getCfgLoader().getConfig().getHashAlgo())) {
                startTask(source, "");
            } else {
                source.sendMessage(plugin.getCfgLoader().getTextConfig().getTotpNotEnabledMessage());
            }

            return CommandResult.success();
        }

        Collection<String> passwords = args.getAll("password");
        List<String> indexPasswords = Lists.newArrayList(passwords);
        String password = indexPasswords.get(0);
        if (password.equals(indexPasswords.get(1))) {
            if (password.length() >= plugin.getCfgLoader().getConfig().getMinPasswordLength()) {
                //Check if the first two passwords are equal to prevent typos
                startTask(source, password);
            } else {
                source.sendMessage(plugin.getCfgLoader().getTextConfig().getTooShortPasswordMessage());
            }
        } else {
            source.sendMessage(plugin.getCfgLoader().getTextConfig().getUnequalPasswordsMessage());
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
