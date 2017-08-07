package org.soraworld.authme.tasks;

import org.soraworld.authme.Account;
import org.soraworld.authme.Authme;
import org.spongepowered.api.command.CommandSource;

import java.util.UUID;

public class ResetPwTask implements Runnable {

    private final Authme plugin = Authme.getInstance();

    private final CommandSource src;
    private final Object accountIndentifer;
    private final String password;

    public ResetPwTask(CommandSource src, UUID uuid, String password) {
        this.src = src;
        this.accountIndentifer = uuid;
        this.password = password;
    }

    public ResetPwTask(CommandSource src, String playerName, String password) {
        this.src = src;
        this.accountIndentifer = playerName;
        this.password = password;
    }

    @Override
    public void run() {
        Account account;
        if (accountIndentifer instanceof String) {
            account = plugin.getDatabase().loadAccount((String) accountIndentifer);
        } else {
            account = plugin.getDatabase().loadAccount((UUID) accountIndentifer);
        }

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
    }
}
