package org.soraworld.account.commands;

import com.google.common.collect.Lists;
import org.soraworld.account.data.Account;
import org.soraworld.account.AuthAccount;
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

public class ChangePasswordCommand implements CommandExecutor {

    private final AuthAccount plugin = AuthAccount.getInstance();

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        if (!(source instanceof Player)) {
            source.sendMessage(plugin.loader().getTextConfig().getPlayersOnlyActionMessage());
            return CommandResult.empty();
        }

        if (plugin.loader().config().isPlayerPermissions()
                && !source.hasPermission(plugin.plugin().getId() + ".command.changepw")) {
            throw new CommandPermissionException();
        }

        Account account = plugin.getDatabase().getAccountIfPresent((Player) source);
        if (account == null || !account.isOnline()) {
            source.sendMessage(plugin.loader().getTextConfig().getNotLoggedInMessage());
            return CommandResult.empty();
        }

        Collection<String> passwords = args.getAll("password");
        List<String> indexPasswords = Lists.newArrayList(passwords);
        String password = indexPasswords.get(0);
        if (password.equals(indexPasswords.get(1))) {
            try {
                //Check if the first two passwords are equal to prevent typos
                String hash = plugin.getHasher().hash(password);
                Sponge.getScheduler().createTaskBuilder()
                        //we are executing a SQL Query which is blocking
                        .async()
                        .execute(() -> {
                            account.setPasswordHash(hash);
                            boolean success = plugin.getDatabase().save(account);
                            if (success) {
                                source.sendMessage(plugin.loader().getTextConfig().getChangePasswordMessage());
                            } else {
                                source.sendMessage(plugin.loader().getTextConfig().getErrorCommandMessage());
                            }
                        })
                        .name("Register Query")
                        .submit(plugin);
            } catch (Exception ex) {
                plugin.getLogger().error("Error creating hash on change password", ex);
                source.sendMessage(plugin.loader().getTextConfig().getErrorCommandMessage());
            }
        } else {
            source.sendMessage(plugin.loader().getTextConfig().getUnequalPasswordsMessage());
        }

        return CommandResult.success();
    }
}
