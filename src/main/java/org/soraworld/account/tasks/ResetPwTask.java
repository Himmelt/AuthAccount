package org.soraworld.account.tasks;

import org.soraworld.account.data.Account;
import org.soraworld.account.AuthAccount;
import org.spongepowered.api.command.CommandSource;

import java.util.UUID;

public class ResetPwTask implements Runnable {

    private final AuthAccount plugin = AuthAccount.getInstance();

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
