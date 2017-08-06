package org.soraworld.authme.commands;

import com.google.common.base.Charsets;
import org.soraworld.authme.Authme;
import org.soraworld.authme.tasks.ForceRegTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Optional;
import java.util.UUID;

public class ForceRegisterCommand implements CommandExecutor {

    private static final String UUID_REGEX
            = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";
    private static final String VALID_USERNAME = "^\\w{2,16}$";

    private final Authme plugin = Authme.getInstance();

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        String accountId = args.<String>getOne("account").get();
        String password = args.<String>getOne("password").get();
        if (accountId.matches(UUID_REGEX)) {
            onUuidRegister(accountId, src, password);

            return CommandResult.success();
        } else if (accountId.matches(VALID_USERNAME)) {
            onNameRegister(src, accountId, password);
            return CommandResult.success();
        }

        return CommandResult.success();
    }

    private void onNameRegister(CommandSource src, String accountId, String password) {
        Optional<Player> player = Sponge.getServer().getPlayer(accountId);
        if (player.isPresent()) {
            src.sendMessage(plugin.getCfgLoader().getTextConfig().getForceRegisterOnlineMessage());
        } else {
            UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + accountId).getBytes(Charsets.UTF_8));

            Sponge.getScheduler().createTaskBuilder()
                    //Async as it could run a SQL query
                    .async()
                    .execute(new ForceRegTask(src, offlineUUID, password))
                    .submit(plugin);
        }
    }

    private void onUuidRegister(String accountId, CommandSource src, String password) {
        //check if the account is an UUID
        UUID uuid = UUID.fromString(accountId);
        Optional<Player> player = Sponge.getServer().getPlayer(uuid);
        if (player.isPresent()) {
            src.sendMessage(plugin.getCfgLoader().getTextConfig().getForceRegisterOnlineMessage());
        } else {
            Sponge.getScheduler().createTaskBuilder()
                    //Async as it could run a SQL query
                    .async()
                    .execute(new ForceRegTask(src, uuid, password))
                    .submit(plugin);
        }
    }
}

