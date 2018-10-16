package org.soraworld.account.commands;

import org.soraworld.account.data.Account;
import org.soraworld.account.AuthAccount;
import org.soraworld.account.tasks.ResetPwTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Optional;
import java.util.UUID;

public class ResetPasswordCommand implements CommandExecutor {

    private static final String UUID_REGEX
            = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";
    private static final String VALID_USERNAME = "^\\w{2,16}$";

    private final AuthAccount plugin = AuthAccount.getInstance();

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        String accountId = args.<String>getOne("account").get();
        String password = args.<String>getOne("password").get();
        if (accountId.matches(UUID_REGEX)) {
            onUuidReset(accountId, src, password);

            return CommandResult.success();
        } else if (accountId.matches(VALID_USERNAME)) {
            onNameReset(src, accountId, password);
            return CommandResult.success();
        }

        return CommandResult.success();
    }

    private void onNameReset(CommandSource src, String accountId, String password) {
        Optional<Player> player = Sponge.getServer().getPlayer(accountId);
        if (player.isPresent()) {
            Account account = plugin.getDatabase().getAccountIfPresent(player.get());
            if (account == null) {
                src.sendMessage(plugin.loader().getTextConfig().getAccountNotFound());
            } else {
                try {
                    account.setPasswordHash(plugin.getHasher().hash(password));
                    src.sendMessage(plugin.loader().getTextConfig().getChangePasswordMessage());
                } catch (Exception ex) {
                    plugin.getLogger().error("Error creating hash", ex);
                    src.sendMessage(plugin.loader().getTextConfig().getErrorCommandMessage());
                }
            }
        } else {
            //check if the account is a valid player name
            Sponge.getScheduler().createTaskBuilder()
                    //Async as it could run a SQL query
                    .async()
                    .execute(new ResetPwTask(src, accountId, password))
                    .submit(plugin);
        }
    }

    private void onUuidReset(String accountId, CommandSource src, String password) {
        //check if the account is an UUID
        UUID uuid = UUID.fromString(accountId);
        Optional<Player> player = Sponge.getServer().getPlayer(uuid);
        if (player.isPresent()) {
            Account account = plugin.getDatabase().getAccountIfPresent(player.get());
            if (account == null) {
                src.sendMessage(plugin.loader().getTextConfig().getAccountNotFound());
            } else {
                try {
                    account.setPasswordHash(plugin.getHasher().hash(password));
                    src.sendMessage(plugin.loader().getTextConfig().getChangePasswordMessage());
                } catch (Exception ex) {
                    plugin.getLogger().error("Error creating hash", ex);
                    src.sendMessage(plugin.loader().getTextConfig().getErrorCommandMessage());
                }
            }
        } else {
            Sponge.getScheduler().createTaskBuilder()
                    //Async as it could run a SQL query
                    .async()
                    .execute(new ResetPwTask(src, uuid, password))
                    .submit(plugin);
        }
    }
}
